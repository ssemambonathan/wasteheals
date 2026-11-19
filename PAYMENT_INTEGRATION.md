# WasteHeals Payment Integration (Mobile Money & Regulatory Posture)

## 1. Current Pilot Posture: Direct MoMo Sandbox
- **MTN Mobile Money** & **Airtel Money** disbursements operate via `MoMoPaymentEngine` in **Sandbox Simulation Mode** (`walletEnabled = false`).
- No live MoMo API keys or merchant secrets are hardcoded in the APK. Live production rollout requires:
  1. Server-side webhook & disbursement proxy (Cloud Run / Cloud Functions) holding encrypted MTN/Airtel API keys and webhook HMAC secrets.
  2. Partner aggregator / Bank of Uganda National Payment Systems (NPS) compliance review before enabling any stored-value wallet (`walletEnabled` remains `false`).

## 2. State Machine & Failure Handling
- **States**: `CREATED -> VALIDATING -> SUBMITTED -> PENDING -> SUCCESS | FAILED | UNKNOWN`.
- **Timeout Safety**: A network timeout (`NETWORK_TIMEOUT`) transitions the payout to `UNKNOWN` with `reconciliationStatus = UNRESOLVED` and `canRetryAutomatically = false`. No automatic retry or duplicate disbursement is permitted.
- **Accounting Isolation**: `DoubleEntryAccountingEngine.forPickerPayoutSuccess` (`DR Picker Payable / CR Operating Cash`) is posted **only** on verified `SUCCESS`.
- **Webhook Security**: `MoMoPaymentEngine.verifyCallback` verifies constant-time HMAC-SHA256 signatures, `idempotencyKey`, `amountUgxMinor`, `currency` (`UGX`), masked destination, and valid state transitions (blocking duplicate or out-of-order callbacks).
