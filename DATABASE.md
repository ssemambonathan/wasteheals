# WasteHeals Database Design

## 1. Local Offline Store (Room SQLite — `wasteheals_offline.db`)
- **`offline_collections`**: Stores field-captured weighings (`PENDING_SYNC`, `SYNCING`, `SYNCED`, `CONFLICT_DUPLICATE`, `SYNC_FAILED`). Enforces a unique SQLite index on `idempotencyKey` (`OnConflictStrategy.IGNORE`) so repeated taps or retries never duplicate local records.
- **`scale_calibrations`**: Tracks Kampala hub weighing scales (`scaleId`, `hubId`, `lastCalibratedEpochMs`, `maxCapacityKgMilli`, `status`).

## 2. Cloud Firestore Enterprise Schema (`/firebase-blueprint.json`)
All collections live inside the named Firestore database configured in `res/values/firebase_applet_config.xml`:

| Collection | Purpose | Access / Query Alignment |
|---|---|---|
| `admins/{uid}` | DB-backed admin markers | Owner/Admin `get`; `update, delete: if false` |
| `users/{userId}` | Non-PII `User` profile (`role`, `roles` in `picker`/`collector`/`admin`/`health_provider`, `requestedRole`, `authProvider`, `assignedRoles`) | Owner/Admin `get`; `viewerUids` scoped `list`; non-admins cannot self-escalate |
| `users/{userId}/private/{docId}` | Isolated PII (`email`, `phone`, `nationalIdRef`, `momoDestinationMasked`) | Strictly owner or admin |
| `hubs/{hubId}` | Kampala hub network | Published query (`published == true`) |
| `materials/{materialId}` | Recyclable, organic & residual catalog | Published query |
| `purchase_rates/{rateId}` | Immutable versioned buy/sell rates | Published query; Maker-Checker updates |
| `financial_policies/{policyId}` | Versioned HC rate, surplus split %, `walletEnabled=false` | Published query; Maker-Checker updates |
| `waste_deliveries/{deliveryId}` | Deterministic `del_{idempotencyKey}` deliveries | `viewerUids` scoped; immutable post-`ACCEPTED` |
| `inventory_batches/{batchId}` | Traceable stock (`received = available + sold + rejected + disposed`) | `viewerUids` scoped |
| `compost_batches/{batchId}` | Organic windrow moisture, temp, shrinkage, yield | `viewerUids` scoped |
| `waste_sales/{saleId}` | Commercial buyer sales & HC accrual/funding | `viewerUids` scoped |
| `journal_entries/{entryId}` | Balanced ledger headers (`totalDebits == totalCredits > 0`) | Append-only; `delete: if false` |
| `journal_lines/{lineId}` | Single-sided debit or credit lines | Immutable (`update, delete: if false`) |
| `picker_earnings/{earningId}` | Guaranteed picker payables | Locked once `SETTLED` |
| `picker_payouts/{payoutId}` | MoMo payout lifecycle & reconciliation state | Locked once `SUCCESS`; Maker-Checker gated |
| `healthcare_clinics/{clinicId}` | Partner clinic directory | Published query |
| `healthcare_vouchers/{voucherId}` | 14-day single-use clinic vouchers | `viewerUids` / `pickerUid` scoped |
| `healthcare_claims/{claimId}` | Clinic redemption claims | `viewerUids` scoped; reviewer != clinic owner |
| `households/{householdId}` | Household separation schedules & non-cash points | `viewerUids` / `ownerUid` scoped |
| `disputes/{disputeId}` | Formal appeals & investigation notes | `viewerUids` / `reporterUid` scoped |
| `approval_requests/{requestId}` | Maker-Checker queue (`makerUid != checkerUid`) | `viewerUids` scoped |
| `audit_logs/{logId}` | Append-only operational & financial audit log | Immutable (`update, delete: if false`) |
