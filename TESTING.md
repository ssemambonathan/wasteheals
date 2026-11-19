# WasteHeals Testing & Verification Guide

## 1. Automated Test Suites Implemented

### A. Financial & Domain Engine Unit Tests (`WasteHealsFinancialEnginesTest.kt`)
- **BigDecimal Weighing & Pricing**: Verifies `25.755 kg - 1.255 kg tare - 0.500 kg deduction = 24.000 kg @ UGX 900 = UGX 21,600` with zero float drift, plus rejection of `tare >= gross`.
- **Double-Entry Ledger & `UGX 30/kg` Healthcare Cash**: Verifies delivery acceptance (`DR Inventory / CR Picker Payable`), confirmed sale (`UGX 3,000` accrued unfunded for `100 kg`), and buyer receipt (`UGX 3,000` transferred to Restricted Healthcare Cash) keep `Debits == Credits`.
- **Surplus & Liquidity Protection Gate**: Verifies zero/negative surplus and failed working-capital liquidity gates force `pickerRewardPoolUgx == 0`, and verifies valid surplus distributes exact 30% pool across normalized picker scores.
- **MoMo State Machine & Webhook HMAC**: Verifies `NETWORK_TIMEOUT -> UNKNOWN + UNRESOLVED`, HMAC-SHA256 signature validation, and rejection of duplicate/out-of-order callbacks.

### B. Robolectric & Room Offline Persistence Tests (`ExampleRobolectricTest.kt`)
- Verifies `app_name == "WasteHeals"` and `firestore_database_id` resource presence.
- Verifies Room `WasteHealsDatabase` unique `idempotencyKey` index deduplicates repeated offline collection writes (`insertIfNew` returns `-1L` on duplicate).

### C. Firestore Zero-Trust Rules Suite (`firestore.test.js`)
Executed against the local Firebase Firestore Emulator (`@firebase/rules-unit-testing`):
1. Unauthenticated read/write denial across all collections.
2. Self-onboarding privilege escalation block (`SUPER_ADMIN` rejected) & `/users/{uid}/private/pii` isolation.
3. Unverified email admin spoofing (`email_verified: false`) rejection.
4. Maker-Checker enforcement (`makerUid != checkerUid` on policies).
5. Double-entry journal balance check and immutability (`update`/`delete` denied).
6. Participant `viewerUids` scoped query enforcement.

## 2. Running All Tests
```bash
gradle :app:testDebugUnitTest --console=plain
firebase emulators:exec --only firestore "node --test firestore.test.js"
```
