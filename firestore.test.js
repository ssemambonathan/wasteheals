const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_nathan_01";
const ADMIN_EMAIL = "nathanssemmambo08@gmail.com";
const AGENT_UID = "agent_kampala_01";
const PICKER_UID = "picker_nakawa_01";
const CHECKER_UID = "finance_checker_02";
const OUTSIDER_UID = "outsider_99";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 5000) {
  return new Date(Date.now() - msOffset);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any WasteHeals collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("waste_deliveries").get());
  await assertFails(unauthDb.collection("journal_entries").get());
  await assertFails(unauthDb.collection("users").doc("user_1").get());
  await assertFails(
    unauthDb.collection("materials").doc("mat_pet").set({
      code: "PET",
      name: "PET Plastic",
      category: "RECYCLABLE",
      unit: "kg",
      grades: ["A", "B"],
      recoveryRoute: "Mechanical Recycling",
      purchaseEligible: true,
      buyerEligible: true,
      co2FactorMilli: 1800,
      published: true,
      ownerUid: PICKER_UID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("User self-onboarding blocks privilege escalation and isolates private PII", async () => {
  const pickerDb = testEnv.authenticatedContext(PICKER_UID, {
    email: "picker@wasteheals.test",
    email_verified: true,
  }).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID, {
    email: "outsider@wasteheals.test",
    email_verified: true,
  }).firestore();
  const now = pastDate();

  // Safe self-onboarding as PICKER via email/phone succeeds
  await assertSucceeds(
    pickerDb.collection("users").doc(PICKER_UID).set({
      userId: PICKER_UID,
      displayName: "Grace Namukasa",
      role: "picker",
      roles: ["picker"],
      requestedRole: "collector",
      authProvider: "email",
      assignedRoles: ["PICKER"],
      activeRole: "PICKER",
      status: "ACTIVE",
      preferredLanguage: "en",
      paymentPreference: "DIRECT_MOMO",
      viewerUids: [PICKER_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Self-assigning SUPER_ADMIN or canonical admin role fails
  await assertFails(
    outsiderDb.collection("users").doc(OUTSIDER_UID).set({
      userId: OUTSIDER_UID,
      displayName: "Attacker",
      role: "admin",
      roles: ["admin"],
      authProvider: "phone",
      assignedRoles: ["SUPER_ADMIN"],
      activeRole: "SUPER_ADMIN",
      status: "ACTIVE",
      viewerUids: [OUTSIDER_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Non-admin cannot self-escalate role/roles on update
  await assertFails(
    pickerDb.collection("users").doc(PICKER_UID).update({
      role: "admin",
      roles: ["picker", "admin"],
      updatedAt: pastDate(1000),
    })
  );

  // Verified bootstrap admin CAN assign collector / health_provider / admin roles
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    adminDb.collection("users").doc(PICKER_UID).update({
      role: "health_provider",
      roles: ["picker", "collector", "admin", "health_provider"],
      assignedRoles: ["HEALTHCARE_MANAGER", "PICKER"],
      activeRole: "HEALTHCARE_MANAGER",
      updatedAt: pastDate(1000),
    })
  );

  // Private PII write by owner succeeds; read by outsider fails
  await assertSucceeds(
    pickerDb.collection("users").doc(PICKER_UID).collection("private").doc("pii").set({
      userId: PICKER_UID,
      email: "picker@wasteheals.test",
      phone: "+256770000111",
      momoDestinationMasked: "+25677***111",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    outsiderDb.collection("users").doc(PICKER_UID).collection("private").doc("pii").get()
  );
  await assertFails(
    outsiderDb.collection("users").doc(PICKER_UID).collection("private").get()
  );
});

test("Email spoofing with email_verified: false cannot access bootstrap admin privileges", async () => {
  const spoofedDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const now = pastDate();

  await assertFails(
    spoofedDb.collection("hubs").doc("hub_nakawa").set({
      code: "NAK-01",
      name: "Nakawa Hub",
      district: "Kampala",
      managerUid: "spoof_uid",
      ownerUid: "spoof_uid",
      published: true,
      status: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Maker-Checker enforcement rejects self-approval on financial policies and approval requests", async () => {
  const makerDb = testEnv.authenticatedContext(AGENT_UID).firestore();
  const checkerDb = testEnv.authenticatedContext(CHECKER_UID).firestore();
  const now = pastDate();

  // Maker submits v2 financial policy as PENDING_APPROVAL
  await assertSucceeds(
    makerDb.collection("financial_policies").doc("pol_v2").set({
      version: 2,
      healthcareUgxPerKg: 35,
      pickerRewardPct: 30,
      operationsPct: 35,
      contingencyPct: 20,
      expansionPct: 15,
      minWorkingCapitalUgx: 5000000,
      walletEnabled: false,
      status: "PENDING_APPROVAL",
      published: false,
      makerUid: AGENT_UID,
      ownerUid: AGENT_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Maker cannot self-approve their own policy
  await assertFails(
    makerDb.collection("financial_policies").doc("pol_v2").update({
      status: "APPROVED",
      published: true,
      checkerUid: AGENT_UID,
      updatedAt: pastDate(1000),
    })
  );

  // Distinct Checker CAN approve
  await assertSucceeds(
    checkerDb.collection("financial_policies").doc("pol_v2").update({
      status: "APPROVED",
      published: true,
      checkerUid: CHECKER_UID,
      updatedAt: pastDate(1000),
    })
  );
});

test("Double-entry journal entries must balance and cannot be mutated or deleted", async () => {
  const agentDb = testEnv.authenticatedContext(AGENT_UID).firestore();
  const now = pastDate();

  // Unbalanced entry (debits != credits) fails
  await assertFails(
    agentDb.collection("journal_entries").doc("je_bad").set({
      entryRef: "JE-2026-0001",
      periodId: "2026-11",
      eventType: "DELIVERY_ACCEPTANCE",
      sourceId: "del_1",
      idempotencyKey: "idem_je_bad_001",
      description: "Unbalanced test",
      totalDebitsUgxMinor: 12000,
      totalCreditsUgxMinor: 11000,
      currency: "UGX",
      postedByUid: AGENT_UID,
      status: "POSTED",
      viewerUids: [AGENT_UID],
      createdAt: now,
    })
  );

  // Balanced entry succeeds
  await assertSucceeds(
    agentDb.collection("journal_entries").doc("je_ok").set({
      entryRef: "JE-2026-0002",
      periodId: "2026-11",
      eventType: "DELIVERY_ACCEPTANCE",
      sourceId: "del_1",
      idempotencyKey: "idem_je_ok_002",
      description: "Accepted 10kg PET at UGX 800/kg",
      totalDebitsUgxMinor: 8000,
      totalCreditsUgxMinor: 8000,
      currency: "UGX",
      postedByUid: AGENT_UID,
      status: "POSTED",
      viewerUids: [AGENT_UID],
      createdAt: now,
    })
  );

  // Silent edit or delete of posted journal entry fails
  await assertFails(
    agentDb.collection("journal_entries").doc("je_ok").update({
      totalDebitsUgxMinor: 9000,
      totalCreditsUgxMinor: 9000,
    })
  );
  await assertFails(agentDb.collection("journal_entries").doc("je_ok").delete());
});

test("Scoped queries succeed for participants while unfiltered list queries fail", async () => {
  const agentDb = testEnv.authenticatedContext(AGENT_UID).firestore();
  const pickerDb = testEnv.authenticatedContext(PICKER_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const now = pastDate();

  await assertSucceeds(
    agentDb.collection("waste_deliveries").doc("del_100").set({
      idempotencyKey: "idem_del_100_abc",
      originType: "PICKER_HUB",
      hubId: "hub_nakawa",
      pickerUid: PICKER_UID,
      pickerName: "Grace Namukasa",
      agentUid: AGENT_UID,
      materialId: "mat_pet",
      materialName: "PET Plastic",
      grade: "A",
      grossKgMilli: 10500,
      tareKgMilli: 500,
      netKgMilli: 10000,
      deductionKgMilli: 0,
      acceptedKgMilli: 10000,
      rateIdSnapshot: "rate_pet_a_v1",
      unitPriceUgxMinor: 800,
      totalEarningsUgxMinor: 8000,
      currency: "UGX",
      receiptRef: "WH-REC-0100",
      verificationStatus: "ACCEPTED",
      originalEventEpochMs: Date.now() - 5000,
      deviceId: "android_dev_01",
      viewerUids: [AGENT_UID, PICKER_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Scoped participant query succeeds
  await assertSucceeds(
    pickerDb.collection("waste_deliveries").where("viewerUids", "array-contains", PICKER_UID).get()
  );

  // Unfiltered query or outsider query fails
  await assertFails(pickerDb.collection("waste_deliveries").get());
  await assertFails(outsiderDb.collection("waste_deliveries").doc("del_100").get());
});
