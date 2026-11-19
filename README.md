# WasteHeals

> *"Every kilogram you collect, protects your health."*

WasteHeals is an Android (Kotlin + Jetpack Compose + Material 3) community waste management, circular economy, double-entry financial, organic recovery, and picker healthcare application engineered for the **Kampala, Uganda pilot (`UGX`)**.

## Core Capabilities Implemented

1. **Authentication & Role-Based Operational Suite (`picker`, `collector`, `admin`, `health_provider`)**
   - Supports **Email** (`EmailAuthProvider`), **Phone Number SMS OTP** (`PhoneAuthProvider`), and **Google Sign-In** (`CredentialManager`). *(Note: Enable Email/Password and Phone Sign-In methods in Firebase Console -> Authentication -> Sign-in method for live email/SMS flows.)*
   - The `User` (`UserAccount`) model in Firestore `/users/{uid}` stores canonical roles (`picker`, `collector`, `admin`, `health_provider`) plus `authProvider`, `requestedRole`, and workspace-level `assignedRoles` across 15 operational roles / 8 workspaces.
   - Workspace switching (`RoleWorkspaceChip` / `RoleSwitcherSheet`) and admin RBAC role assignment (`Maker-Checker -> User Roles & RBAC`) are enforced by zero-trust `firestore.rules`.

2. **Offline-First Field Collection & Calibrated Weighing**
   - Field collections are saved locally to **Room SQLite (`OfflineCollectionEntity`)** with deterministic UUID `idempotencyKey`, immutable purchase rate snapshots (`rateIdSnapshot`, `unitPriceUgxMinor`), gross/tare/moisture/contamination calculations via **`BigDecimal`** (`MoneyAndWeight` & `PricingAndWeighingEngine`), and zero-permission `ActivityResultContracts.PickVisualMedia` photo evidence.
   - Offline events produce **provisional receipts** (`WH-PROV-*`) and **never auto-post final financial liabilities** until idempotently synced to Firestore (`del_{idempotencyKey}`) and verified at a Kampala hub.

3. **Hub Verification, Mass-Balanced Inventory & Organic Composting**
   - Hub acceptance atomically updates `waste_deliveries`, creates traceable `inventory_batches`, creates guaranteed `picker_earnings`, posts balanced `journal_entries`/`journal_lines` (`DR Inventory / CR Guaranteed Picker Payable`), and logs an append-only `audit_logs` entry.
   - Organic windrow batches (`compost_batches`) track input weight, moisture %, turning temperature, shrinkage loss, rejections, and cured compost yield.

4. **Commercial Buyer Sales & `UGX 30/kg` Healthcare Fund**
   - Confirmed buyer weighbridge sales (`waste_sales`) accrue `UGX 30/kg` on confirmed sold weight into **Healthcare Accrued (Unfunded)**.
   - Recording buyer cash receipts proportionally transfers cash from Operating Cash into **Restricted Healthcare Cash Fund**.
   - Single-use clinic vouchers (`healthcare_vouchers`) are strictly blocked unless funded Restricted Healthcare Cash covers the voucher ceiling.

5. **Direct Mobile Money Sandbox & Maker-Checker Governance**
   - Direct MTN Mobile Money & Airtel Money Sandbox adapter (`MoMoPaymentEngine`) with failure scenario testing (Success, Gateway Timeout -> `UNKNOWN` lock, Duplicate Webhook, Out-of-Order Callback, Insufficient Float, Invalid MSISDN) and HMAC-SHA256 webhook verification.
   - **Stored-value wallet is feature-flagged OFF (`walletEnabled = false`)** pending Bank of Uganda National Payment Systems (NPS) Act legal clearance.
   - Policy updates (`financial_policies`) and payout disbursements enforce **Maker-Checker** separation (`makerUid != checkerUid`).

## Build & Test

- Compile & run JVM + Robolectric tests:
  ```bash
  gradle :app:testDebugUnitTest
  ```
- Run Firebase Firestore security rules unit tests (via Firestore Emulator):
  ```bash
  firebase emulators:exec --only firestore "node --test firestore.test.js"
  ```
