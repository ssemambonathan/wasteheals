package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.ScaleCalibrationEntity
import com.example.data.local.WasteHealsDatabase
import com.example.domain.model.AccountStatus
import com.example.domain.model.ApprovalRequest
import com.example.domain.model.ApprovalStatus
import com.example.domain.model.CollectionHub
import com.example.domain.model.FinancialPolicy
import com.example.domain.model.HealthcareClinic
import com.example.domain.model.MaterialCategory
import com.example.domain.model.PurchaseRate
import com.example.domain.model.SettlementMethod
import com.example.domain.model.UserAccount
import com.example.domain.model.UserPrivatePii
import com.example.domain.model.UserRole
import com.example.domain.model.WasteMaterial
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class WasteHealsCatalogRepository(
  private val db: FirebaseFirestore,
  private val localDb: WasteHealsDatabase,
  private val auth: FirebaseAuth = Firebase.auth
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id)),
    WasteHealsDatabase.getInstance(context.applicationContext)
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid ?: throw IllegalStateException("Google Sign-In required.")

  private fun isBootstrapAdmin(): Boolean {
    val user = auth.currentUser ?: return false
    return user.email == BOOTSTRAP_ADMIN_EMAIL && user.isEmailVerified
  }

  fun observeUserProfile(uid: String): Flow<UserAccount?> =
    db.collection("users").document(uid).snapshots()
      .map { it.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        rethrowIfCancellation(e)
        handleFirestoreError(e, OperationType.GET, "users/$uid")
        throw e
      }

  fun observeUserPrivatePii(uid: String): Flow<UserPrivatePii?> =
    db.collection("users").document(uid).collection("private").document("pii").snapshots()
      .map { it.toObject(UserPrivatePii::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        rethrowIfCancellation(e)
        handleFirestoreError(e, OperationType.GET, "users/$uid/private/pii")
        throw e
      }

  fun observePublishedHubs(): Flow<List<CollectionHub>> =
    db.collection("hubs").whereEqualTo("published", true).snapshots()
      .map { it.toObjects(CollectionHub::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        rethrowIfCancellation(e)
        handleFirestoreError(e, OperationType.LIST, "hubs")
        throw e
      }

  fun observePublishedMaterials(): Flow<List<WasteMaterial>> =
    db.collection("materials").whereEqualTo("published", true).snapshots()
      .map { it.toObjects(WasteMaterial::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        rethrowIfCancellation(e)
        handleFirestoreError(e, OperationType.LIST, "materials")
        throw e
      }

  fun observeApprovedRates(): Flow<List<PurchaseRate>> =
    db.collection("purchase_rates").whereEqualTo("published", true).snapshots()
      .map { it.toObjects(PurchaseRate::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        rethrowIfCancellation(e)
        handleFirestoreError(e, OperationType.LIST, "purchase_rates")
        throw e
      }

  fun observeActivePolicy(): Flow<FinancialPolicy?> =
    db.collection("financial_policies").whereEqualTo("published", true).snapshots()
      .map { snap ->
        snap.toObjects(FinancialPolicy::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .maxByOrNull { it.version }
      }
      .catch { e ->
        rethrowIfCancellation(e)
        handleFirestoreError(e, OperationType.LIST, "financial_policies")
        throw e
      }

  fun observePublishedClinics(): Flow<List<HealthcareClinic>> =
    db.collection("healthcare_clinics").whereEqualTo("published", true).snapshots()
      .map { it.toObjects(HealthcareClinic::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        rethrowIfCancellation(e)
        handleFirestoreError(e, OperationType.LIST, "healthcare_clinics")
        throw e
      }

  fun observeAllUsers(uid: String): Flow<List<UserAccount>> =
    (if (isBootstrapAdmin()) db.collection("users")
    else db.collection("users").whereArrayContains("viewerUids", uid))
      .snapshots()
      .map { it.toObjects(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        rethrowIfCancellation(e)
        handleFirestoreError(e, OperationType.LIST, "users")
        throw e
      }

  fun observeScaleCalibrations(): Flow<List<ScaleCalibrationEntity>> =
    localDb.offlineCollectionDao().observeScaleCalibrations()

  suspend fun ensureUserOnboarded(): Result<Unit> {
    var path = "users"
    return try {
      val user = auth.currentUser ?: error("Not signed in")
      val uid = user.uid
      path = "users/$uid"
      val docRef = db.collection("users").document(uid)
      val snap = docRef.get().await()
      if (!snap.exists()) {
        val isBootAdmin = isBootstrapAdmin()
        val initialRoles = if (isBootAdmin) {
          UserRole.entries.map { it.name }
        } else {
          listOf(UserRole.PICKER.name, UserRole.HOUSEHOLD.name)
        }
        val activeRole = if (isBootAdmin) UserRole.SUPER_ADMIN.name else UserRole.PICKER.name
        val display = (user.displayName?.takeIf { it.isNotBlank() } ?: "WasteHeals Member").take(80)
        val userPayload = mapOf(
          "userId" to uid,
          "displayName" to display,
          "assignedRoles" to initialRoles,
          "activeRole" to activeRole,
          "assignedHubId" to "hub_nakawa",
          "status" to AccountStatus.ACTIVE.name,
          "preferredLanguage" to "en",
          "paymentPreference" to SettlementMethod.DIRECT_MOMO.name,
          "viewerUids" to listOf(uid),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        val piiPayload = mapOf(
          "userId" to uid,
          "email" to user.email,
          "phone" to "+256770000000",
          "momoDestinationMasked" to "+25677***000",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        val batch = db.batch()
        batch.set(docRef, userPayload)
        batch.set(docRef.collection("private").document("pii"), piiPayload)
        if (isBootAdmin) {
          batch.set(
            db.collection("admins").document(uid),
            mapOf(
              "uid" to uid,
              "email" to user.email,
              "grantedBy" to "BOOTSTRAP",
              "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
          )
        }
        batch.commit().await()
      }
      // Seed scale calibration locally for Kampala hubs
      localDb.offlineCollectionDao().upsertScaleCalibration(
        ScaleCalibrationEntity(
          scaleId = "SCALE-NAK-01",
          hubId = "hub_nakawa",
          lastCalibratedEpochMs = System.currentTimeMillis() - 86_400_000L,
          maxCapacityKgMilli = 500_000L,
          status = "CALIBRATED"
        )
      )
      if (isBootstrapAdmin()) {
        seedPilotCatalogIfEmpty(uid)
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun seedPilotCatalogIfEmpty(uid: String): Result<Unit> {
    return try {
      val existingHubs = db.collection("hubs").whereEqualTo("published", true).limit(1).get().await()
      if (!existingHubs.isEmpty) return Result.success(Unit)

      val batch = db.batch()
      val hubs = listOf(
        Triple("hub_nakawa", "NAK-01", "Nakawa Circular Hub" to "Kampala Central"),
        Triple("hub_kawempe", "KAW-02", "Kawempe Recovery Station" to "Kawempe Division"),
        Triple("hub_makindye", "MAK-03", "Makindye Compost & Sorting Hub" to "Makindye Division")
      )
      hubs.forEach { (id, code, nameDistrict) ->
        batch.set(
          db.collection("hubs").document(id),
          mapOf(
            "code" to code,
            "name" to nameDistrict.first,
            "district" to nameDistrict.second,
            "managerUid" to uid,
            "ownerUid" to uid,
            "published" to true,
            "status" to "ACTIVE",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }

      data class SeedMat(
        val id: String,
        val code: String,
        val name: String,
        val category: MaterialCategory,
        val grades: List<String>,
        val route: String,
        val co2Milli: Long,
        val buyUgx: Long,
        val sellUgx: Long
      )

      val mats = listOf(
        SeedMat("mat_pet", "PET", "Clear PET Bottles", MaterialCategory.RECYCLABLE, listOf("A", "B"), "Mechanical Flake Recycling", 1800L, 900L, 1450L),
        SeedMat("mat_hdpe", "HDPE", "HDPE Jerrycans & Caps", MaterialCategory.RECYCLABLE, listOf("A", "B"), "Pelletization", 1600L, 1100L, 1750L),
        SeedMat("mat_alu", "ALU", "Aluminium Beverage Cans", MaterialCategory.RECYCLABLE, listOf("A"), "Smelter Ingot Recovery", 8200L, 3200L, 4600L),
        SeedMat("mat_card", "OCC", "Corrugated Cardboard", MaterialCategory.RECYCLABLE, listOf("A", "B"), "Pulp Mill Recycling", 950L, 450L, 780L),
        SeedMat("mat_org_food", "ORG-F", "Market & Kitchen Organics", MaterialCategory.ORGANIC, listOf("CLEAN", "MIXED"), "Aerobic Windrow Composting", 620L, 150L, 420L),
        SeedMat("mat_res", "RES", "Residual Non-Recyclable", MaterialCategory.RESIDUAL, listOf("DISPOSAL"), "Authorised Sanitary Disposal", 0L, 0L, 0L)
      )

      mats.forEach { m ->
        batch.set(
          db.collection("materials").document(m.id),
          mapOf(
            "code" to m.code,
            "name" to m.name,
            "category" to m.category.name,
            "unit" to "kg",
            "grades" to m.grades,
            "recoveryRoute" to m.route,
            "purchaseEligible" to (m.buyUgx > 0L),
            "buyerEligible" to (m.sellUgx > 0L),
            "co2FactorMilli" to m.co2Milli,
            "published" to true,
            "ownerUid" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        if (m.buyUgx > 0L) {
          batch.set(
            db.collection("purchase_rates").document("rate_${m.id}_v1"),
            mapOf(
              "materialId" to m.id,
              "materialName" to m.name,
              "grade" to m.grades.first(),
              "hubId" to "hub_nakawa",
              "purchasePriceUgxMinor" to m.buyUgx,
              "buyerSellPriceUgxMinor" to m.sellUgx,
              "currency" to "UGX",
              "version" to 1L,
              "status" to "APPROVED",
              "published" to true,
              "makerUid" to uid,
              "checkerUid" to "SYSTEM_PILOT_GENESIS",
              "ownerUid" to uid,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }
      }

      batch.set(
        db.collection("financial_policies").document("policy_v1"),
        mapOf(
          "version" to 1L,
          "healthcareUgxPerKg" to 30L,
          "pickerRewardPct" to 30L,
          "operationsPct" to 35L,
          "contingencyPct" to 20L,
          "expansionPct" to 15L,
          "minWorkingCapitalUgx" to 2_000_000L,
          "walletEnabled" to false,
          "status" to "APPROVED",
          "published" to true,
          "makerUid" to uid,
          "checkerUid" to "SYSTEM_PILOT_GENESIS",
          "ownerUid" to uid,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )

      val clinics = listOf(
        Triple("clinic_nakawa", "Nakawa Community Health Clinic", "Nakawa Division, Kampala" to "Primary Care, Malaria Test, Wound Dressing, Tetanus"),
        Triple("clinic_kawempe", "Kawempe Good Samaritan Medical", "Kawempe Mbogo Rd, Kampala" to "Outpatient Consult, Lab Panel, Maternal Check, PPE Injury Care")
      )
      clinics.forEach { (id, name, locServ) ->
        batch.set(
          db.collection("healthcare_clinics").document(id),
          mapOf(
            "clinicName" to name,
            "location" to locServ.first,
            "licenseRef" to "UMDPC-KLA-${id.takeLast(4).uppercase()}",
            "servicesSummary" to locServ.second,
            "ownerUid" to uid,
            "published" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }

      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "pilot_seed")
      Result.failure(e)
    }
  }

  suspend fun switchActiveRole(role: UserRole): Result<Unit> {
    val uid = requireUserId()
    val path = "users/$uid"
    return try {
      db.collection("users").document(uid).update(
        mapOf(
          "activeRole" to role.name,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun updatePrivatePii(phone: String, momoMasked: String, emergencyContact: String): Result<Unit> {
    val uid = requireUserId()
    val path = "users/$uid/private/pii"
    return try {
      require(phone.length in 6..32) { "Enter a valid phone number." }
      db.collection("users").document(uid).collection("private").document("pii")
        .update(
          mapOf(
            "phone" to phone.trim(),
            "momoDestinationMasked" to momoMasked.trim().take(40),
            "emergencyContact" to emergencyContact.trim().take(120),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun proposeFinancialPolicyUpdate(
    currentVersion: Long,
    healthcareUgxPerKg: Long,
    pickerRewardPct: Long,
    operationsPct: Long,
    contingencyPct: Long,
    expansionPct: Long,
    minWorkingCapitalUgx: Long
  ): Result<Unit> {
    val uid = requireUserId()
    val nextVer = currentVersion + 1L
    val policyId = "policy_v$nextVer"
    val reqId = "req_policy_v$nextVer"
    return try {
      require(pickerRewardPct + operationsPct + contingencyPct + expansionPct == 100L) {
        "Surplus policy percentages must sum to 100%."
      }
      val batch = db.batch()
      batch.set(
        db.collection("financial_policies").document(policyId),
        mapOf(
          "version" to nextVer,
          "healthcareUgxPerKg" to healthcareUgxPerKg,
          "pickerRewardPct" to pickerRewardPct,
          "operationsPct" to operationsPct,
          "contingencyPct" to contingencyPct,
          "expansionPct" to expansionPct,
          "minWorkingCapitalUgx" to minWorkingCapitalUgx,
          "walletEnabled" to false,
          "status" to "PENDING_APPROVAL",
          "published" to false,
          "makerUid" to uid,
          "ownerUid" to uid,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.set(
        db.collection("approval_requests").document(reqId),
        mapOf(
          "entityType" to "FINANCIAL_POLICY",
          "entityId" to policyId,
          "actionSummary" to "Policy v$nextVer: HC UGX $healthcareUgxPerKg/kg, Reward $pickerRewardPct%",
          "payloadSummary" to "Ops $operationsPct% | Cont $contingencyPct% | Exp $expansionPct% | MinWC UGX $minWorkingCapitalUgx",
          "makerUid" to uid,
          "status" to ApprovalStatus.PENDING.name,
          "viewerUids" to listOf(uid),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "financial_policies/$policyId")
      Result.failure(e)
    }
  }

  private fun rethrowIfCancellation(error: Throwable) {
    val firestoreErr = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreErr == null && error is CancellationException) throw error
  }

  companion object {
    const val BOOTSTRAP_ADMIN_EMAIL = "nathanssemmambo08@gmail.com"
  }
}
