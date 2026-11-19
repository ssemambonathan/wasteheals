# WasteHeals Threat Model & Security Controls

| Threat | Mitigation Implemented | Verification |
|---|---|---|
| **Unauthenticated Data Access** | Global default-deny catch-all (`match /{document=**} { allow read, write: if false; }`) + Google Sign-In via Jetpack `CredentialManager`. | `firestore.test.js` Test 1 |
| **Role Privilege Escalation** | Self-onboarding restricted to `PICKER`/`HOUSEHOLD` (`isSafeSelfRoles`); `users/{uid}` updates block self-modification of `assignedRoles` or `status`. | `firestore.test.js` Test 2 |
| **Unverified Email Admin Spoofing** | `isBootstrapAdmin()` requires BOTH exact email match AND `request.auth.token.email_verified == true`. | `firestore.test.js` Test 3 |
| **PII Exposure** | Sensitive PII (`email`, `phone`, `nationalIdRef`, `momoDestinationMasked`) isolated in `/users/{uid}/private/pii`, accessible only to owner or admin. | `firestore.test.js` Test 2 |
| **Maker-Checker Bypass** | `approval_requests`, `financial_policies`, and `purchase_rates` enforce `request.auth.uid != existing().makerUid` in Firestore rules and repositories. | `firestore.test.js` Test 4 |
| **Ledger Tampering / Imbalance** | `journal_entries` require `totalDebitsUgxMinor == totalCreditsUgxMinor > 0`; `journal_lines` forbid updates/deletes (`update, delete: if false`). | `firestore.test.js` Test 5 |
| **Double Payout / Offline Duplication** | Room unique index on `idempotencyKey` + deterministic Firestore ID `del_{idempotencyKey}`; `picker_earnings` & `picker_payouts` locked once `SETTLED`/`SUCCESS`. | `ExampleRobolectricTest` & `WasteHealsFinancialEnginesTest` |
| **Forged MoMo Webhooks** | `MoMoPaymentEngine.verifyCallback` validates constant-time HMAC-SHA256 (`MessageDigest.isEqual`), amount, currency, idempotencyKey, destination, and state machine transitions. | `WasteHealsFinancialEnginesTest` |
| **Play Policy / Storage Overreach** | Zero `READ_EXTERNAL_STORAGE`/`READ_MEDIA_IMAGES` permissions; uses Android Photo Picker (`PickVisualMedia`). | `AndroidManifest.xml` & `CollectionScreen.kt` |
