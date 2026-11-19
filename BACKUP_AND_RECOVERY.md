# WasteHeals Backup, Recovery & Disaster Resilience

1. **Client-Side Offline Durability**:
   - Field collections captured during Kampala connectivity drops persist in Room SQLite (`wasteheals_offline.db`) with unique `idempotencyKey` constraints.
   - Sync uses deterministic Firestore document IDs (`del_{idempotencyKey}`), preventing duplicate liabilities across retries or device crashes.
2. **Cloud Persistence & Auditability**:
   - Named Firestore database (`ai-studio-android-wastehea-482690c8-0e98-4f96-8d87-e26e89b545ce`) enforces append-only `journal_entries`, `journal_lines`, and `audit_logs` (`allow delete: if false` across all financial & operational records).
   - Scheduled Cloud Firestore managed exports (to a restricted GCS bucket with object versioning) should be enabled in GCP Project `gen-lang-client-0865710117` prior to live Kampala production launch.
3. **Recovery Playbook**:
   - **UNKNOWN MoMo Payout**: Never auto-retry; query provider status using `providerRef` / `idempotencyKey`, then resolve via `reconcileUnknownPayout(payout, confirmedSucceeded)`.
   - **Incorrect Ledger Entry**: Never mutate or delete posted entries; issue an offsetting reversal entry (`POSTED -> REVERSED`).
   - **Compliance Snapshot Export**: Auditors can export Trial Balance & Impact CSV reports directly from the Finance tab (`Share Trial Balance & Impact CSV Report`).
