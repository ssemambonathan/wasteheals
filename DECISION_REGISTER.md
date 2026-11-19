# WasteHeals Architecture & Product Decision Register

| ID | Decision | Rationale | Status |
|---|---|---|---|
| **DR-001** | Native Android (Kotlin/Compose) + Named Firebase Firestore + Room Offline Queue | Provides instant mobile field capture in Kampala low-connectivity zones paired with real-time multi-role cloud sync and zero-trust rules. | Implemented |
| **DR-002** | `BigDecimal` + `Long` whole UGX & `milliKg` (`1000 = 1 kg`) | Eliminates IEEE-754 floating-point rounding drift in weighing, ledger posting, and surplus splits. | Implemented |
| **DR-003** | Direct MoMo Sandbox Payouts; Stored-Value Wallet Feature-Flagged OFF (`walletEnabled = false`) | Avoids unregulated deposit-taking under Uganda's National Payment Systems (NPS) Act until legal clearance and BoU licensing are obtained. | Implemented |
| **DR-004** | Dual-Approval Maker-Checker (`makerUid != checkerUid`) for Policies, Rates & Payouts | Prevents single-actor fraud or unreviewed economic parameter changes. | Implemented |
| **DR-005** | Healthcare Fund (`UGX 30/kg`) Accrued on Confirmed Sale, Funded on Buyer Cash Receipt | Prevents issuing medical vouchers against unpaid buyer receivables or unrealized inventory. | Implemented |
| **DR-006** | Google Sign-In via Jetpack `CredentialManager` (No Anonymous Auth) | Ensures strong verified identity attribution (`email_verified`) for every financial and operational action. | Implemented |
| **DR-007** | Android Photo Picker (`PickVisualMedia`) for Evidence | Zero broad storage permissions (`READ_EXTERNAL_STORAGE` / `READ_MEDIA_*`), fully compliant with Google Play Developer Policy. | Implemented |
