package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.domain.engine.DoubleEntryAccountingEngine
import com.example.domain.engine.MoMoDisbursementRequest
import com.example.domain.engine.MoMoPaymentEngine
import com.example.domain.engine.SandboxScenario
import com.example.domain.model.ApprovalRequest
import com.example.domain.model.ApprovalStatus
import com.example.domain.model.AuditLog
import com.example.domain.model.ClaimStatus
import com.example.domain.model.DisputeRecord
import com.example.domain.model.DisputeStatus
import com.example.domain.model.HealthcareClaim
import com.example.domain.model.HealthcareClinic
import com.example.domain.model.HealthcareVoucher
import com.example.domain.model.Household
import com.example.domain.model.JournalEntry
import com.example.domain.model.JournalLine
import com.example.domain.model.MoMoLifecycleStatus
import com.example.domain.model.MoMoProvider
import com.example.domain.model.PayoutStatus
import com.example.domain.model.PickerEarning
import com.example.domain.model.PickerPayout
import com.example.domain.model.ReconciliationStatus
import com.example.domain.model.VoucherStatus
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class WasteHealsFinanceHealthRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id))
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid ?: throw IllegalStateException("Google Sign-In required.")

  private fun isBootstrapAdmin(): Boolean {
    val user = auth.currentUser ?: return false
    return user.email == WasteHealsCatalogRepository.BOOTSTRAP_ADMIN_EMAIL && user.isEmailVerified
  }

  private fun currentPeriodId(): String =
    SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

  fun observeJournalEntries(uid: String): Flow<List<JournalEntry>> =
    (if (isBootstrapAdmin()) db.collection("journal_entries")
    else db.collection("journal_entries").whereArrayContains("viewerUids", uid))
      .snapshots()
      .map { it.toObjects(JournalEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e -> rethrowIfCancellation(e); handleFirestoreError(e, OperationType.LIST, "journal_entries"); throw e }

  fun observeJournalLines(uid: String): Flow<List<JournalLine>> =
    (if (isBootstrapAdmin()) db.collection("journal_lines")
    else db.collection("journal_lines").whereArrayContains("viewerUids", uid))
      .snapshots()
      .map { it.toObjects(JournalLine::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e -> rethrowIfCancellation(e); handleFirestoreError(e, OperationType.LIST, "journal_lines"); throw e }

  fun observePickerEarnings(uid: String): Flow<List<PickerEarning>> =
    (if (isBootstrapAdmin()) db.collection("picker_earnings")
    else db.collection("picker_earnings").whereArrayContains("viewerUids", uid))
      .snapshots()
      .map { it.toObjects(PickerEarning::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e -> rethrowIfCancellation(e); handleFirestoreError(e, OperationType.LIST, "picker_earnings"); throw e }

  fun observePickerPayouts(uid: String): Flow<List<PickerPayout>> =
    (if (isBootstrapAdmin()) db.collection("picker_payouts")
    else db.collection("picker_payouts").whereArrayContains("viewerUids", uid))
      .snapshots()
      .map { it.toObjects(PickerPayout::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e -> rethrowIfCancellation(e); handleFirestoreError(e, OperationType.LIST, "picker_payouts"); throw e }

  fun observeVouchers(uid: String): Flow<List<HealthcareVoucher>> =
    (if (isBootstrapAdmin()) db.collection("healthcare_vouchers")
    else db.collection("healthcare_vouchers").whereArrayContains("viewerUids", uid))
      .snapshots()
      .map { it.toObjects(HealthcareVoucher::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e -> rethrowIfCancellation(e); handleFirestoreError(e, OperationType.LIST, "healthcare_vouchers"); throw e }

  fun observeClaims(uid: String): Flow<List<HealthcareClaim>> =
    (if (isBootstrapAdmin()) db.collection("healthcare_claims")
    else db.collection("healthcare_claims").whereArrayContains("viewerUids", uid))
      .snapshots()
      .map { it.toObjects(HealthcareClaim::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e -> rethrowIfCancellation(e); handleFirestoreError(e, OperationType.LIST, "healthcare_claims"); throw e }

  fun observeHouseholds(uid: String): Flow<List<Household>> =
    (if (isBootstrapAdmin()) db.collection("households")
    else db.collection("households").whereArrayContains("viewerUids", uid))
      .snapshots()
      .map { it.toObjects(Household::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e -> rethrowIfCancellation(e); handleFirestoreError(e, OperationType.LIST, "households"); throw e }

  fun observeDisputes(uid: String): Flow<List<DisputeRecord>> =
    (if (isBootstrapAdmin()) db.collection("disputes")
    else db.collection("disputes").whereArrayContains("viewerUids", uid))
      .snapshots()
      .map { it.toObjects(DisputeRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e -> rethrowIfCancellation(e); handleFirestoreError(e, OperationType.LIST, "disputes"); throw e }

  fun observeApprovalRequests(uid: String): Flow<List<ApprovalRequest>> =
    (if (isBootstrapAdmin()) db.collection("approval_requests")
    else db.collection("approval_requests").whereArrayContains("viewerUids", uid))
      .snapshots()
      .map { it.toObjects(ApprovalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e -> rethrowIfCancellation(e); handleFirestoreError(e, OperationType.LIST, "approval_requests"); throw e }

  fun observeAuditLogs(uid: String): Flow<List<AuditLog>> =
    (if (isBootstrapAdmin()) db.collection("audit_logs")
    else db.collection("audit_logs").whereArrayContains("viewerUids", uid))
      .snapshots()
      .map { it.toObjects(AuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e -> rethrowIfCancellation(e); handleFirestoreError(e, OperationType.LIST, "audit_logs"); throw e }

  /**
   * Step 1 of Maker-Checker Payout: Creates PickerPayout in CREATED status + ApprovalRequest
   * and marks PickerEarning as QUEUED so it cannot be double-settled.
   */
  suspend fun createPayoutDraft(
    earning: PickerEarning,
    provider: MoMoProvider,
    destinationMasked: String
  ): Result<String> {
    val uid = requireUserId()
    val payoutId = "pay_${earning.id}"
    return try {
      require(earning.payoutStatus == PayoutStatus.UNSETTLED.name || earning.payoutStatus == PayoutStatus.FAILED.name) {
        "Earning already queued, processing, or settled."
      }
      require(earning.amountUgxMinor > 0L) { "Payout amount must be positive." }
      val viewers = (earning.viewerUids + uid).distinct().take(15)
      val idemKey = "momo_${earning.id}_${System.currentTimeMillis()}"
      val batch = db.batch()
      batch.set(
        db.collection("picker_payouts").document(payoutId),
        mapOf(
          "idempotencyKey" to idemKey,
          "earningId" to earning.id,
          "pickerUid" to earning.pickerUid,
          "pickerName" to earning.pickerName,
          "provider" to provider.name,
          "destinationMasked" to destinationMasked.trim().take(40),
          "amountUgxMinor" to earning.amountUgxMinor,
          "currency" to "UGX",
          "lifecycleStatus" to MoMoLifecycleStatus.CREATED.name,
          "reconciliationStatus" to ReconciliationStatus.UNRECONCILED.name,
          "makerUid" to uid,
          "viewerUids" to viewers,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.update(
        db.collection("picker_earnings").document(earning.id),
        mapOf(
          "payoutStatus" to PayoutStatus.QUEUED.name,
          "payoutId" to payoutId,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.set(
        db.collection("approval_requests").document("req_$payoutId"),
        mapOf(
          "entityType" to "PICKER_PAYOUT",
          "entityId" to payoutId,
          "actionSummary" to "MoMo Payout ${earning.pickerName} (${provider.displayName})",
          "payloadSummary" to "UGX ${earning.amountUgxMinor} -> $destinationMasked",
          "makerUid" to uid,
          "status" to ApprovalStatus.PENDING.name,
          "viewerUids" to viewers,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.commit().await()
      Result.success(payoutId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "picker_payouts/$payoutId")
      Result.failure(e)
    }
  }

  /**
   * Executes Sandbox MoMo payout after Maker-Checker verification (checker != maker unless bootstrap admin).
   * Posts accounting entry ONLY on verified SUCCESS; timeouts lock in UNKNOWN/UNRESOLVED.
   */
  suspend fun executeSandboxPayout(
    payout: PickerPayout,
    scenario: SandboxScenario
  ): Result<String> {
    val uid = requireUserId()
    val path = "picker_payouts/${payout.id}"
    return try {
      require(payout.lifecycleStatus != MoMoLifecycleStatus.SUCCESS.name) {
        "Payout already settled."
      }
      val outcome = MoMoPaymentEngine.simulateSandboxDisbursement(
        MoMoDisbursementRequest(
          internalPayoutId = payout.id,
          idempotencyKey = payout.idempotencyKey,
          provider = MoMoProvider.fromString(payout.provider),
          destinationMsisdnMasked = payout.destinationMasked,
          amountUgxMinor = payout.amountUgxMinor
        ),
        scenario
      )
      val viewers = (payout.viewerUids + uid).distinct().take(15)
      val earningStatus = when (outcome.nextLifecycleStatus) {
        MoMoLifecycleStatus.SUCCESS -> PayoutStatus.SETTLED.name
        MoMoLifecycleStatus.FAILED -> PayoutStatus.FAILED.name
        MoMoLifecycleStatus.UNKNOWN -> PayoutStatus.UNKNOWN_RECONCILE.name
        else -> PayoutStatus.PROCESSING.name
      }

      val batch = db.batch()
      batch.update(
        db.collection("picker_payouts").document(payout.id),
        mapOf(
          "lifecycleStatus" to outcome.nextLifecycleStatus.name,
          "providerRef" to outcome.providerRef,
          "failureCode" to outcome.failureCode,
          "failureReason" to outcome.failureReason,
          "reconciliationStatus" to outcome.reconciliationStatus.name,
          "checkerUid" to uid,
          "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
      )
      batch.update(
        db.collection("picker_earnings").document(payout.earningId),
        mapOf(
          "payoutStatus" to earningStatus,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )

      if (outcome.nextLifecycleStatus == MoMoLifecycleStatus.SUCCESS) {
        val entryId = "je_pay_${payout.id}"
        val journal = DoubleEntryAccountingEngine.forPickerPayoutSuccess(
          entryRef = "JE-PAY-${payout.id.takeLast(5).uppercase()}",
          periodId = currentPeriodId(),
          payoutId = payout.id,
          hubId = "hub_nakawa",
          amountUgx = payout.amountUgxMinor,
          pickerName = payout.pickerName
        ).getOrThrow()
        batch.set(
          db.collection("journal_entries").document(entryId),
          mapOf(
            "entryRef" to journal.entryRef,
            "periodId" to journal.periodId,
            "eventType" to journal.eventType,
            "sourceId" to journal.sourceId,
            "idempotencyKey" to journal.idempotencyKey,
            "description" to journal.description,
            "totalDebitsUgxMinor" to journal.totalDebitsUgxMinor,
            "totalCreditsUgxMinor" to journal.totalCreditsUgxMinor,
            "currency" to "UGX",
            "postedByUid" to uid,
            "status" to "POSTED",
            "viewerUids" to viewers,
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
        journal.lines.forEachIndexed { idx, line ->
          batch.set(
            db.collection("journal_lines").document("${entryId}_$idx"),
            mapOf(
              "entryId" to entryId,
              "accountCode" to line.accountCode,
              "accountName" to line.accountName,
              "accountCategory" to line.accountCategory.name,
              "debitUgxMinor" to line.debitUgxMinor,
              "creditUgxMinor" to line.creditUgxMinor,
              "currency" to "UGX",
              "hubId" to line.hubId,
              "postedByUid" to uid,
              "viewerUids" to viewers,
              "createdAt" to FieldValue.serverTimestamp()
            )
          )
        }
      }
      batch.commit().await()
      Result.success(outcome.nextLifecycleStatus.name)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  /**
   * Resolves an UNKNOWN / UNRESOLVED payout via the Reconciliation Engine after querying provider status.
   */
  suspend fun reconcileUnknownPayout(payout: PickerPayout, confirmedSucceeded: Boolean): Result<Unit> {
    return executeSandboxPayout(
      payout,
      if (confirmedSucceeded) SandboxScenario.IMMEDIATE_SUCCESS else SandboxScenario.INSUFFICIENT_FLOAT
    ).map { }
  }

  suspend fun approveOrRejectRequest(
    request: ApprovalRequest,
    approve: Boolean,
    rejectionReason: String?
  ): Result<Unit> {
    val uid = requireUserId()
    val path = "approval_requests/${request.id}"
    return try {
      require(request.status == ApprovalStatus.PENDING.name) { "Request already finalized." }
      require(uid != request.makerUid) {
        "Separation of duties violation: Maker (${request.makerUid.take(6)}) cannot approve own request."
      }
      val newStatus = if (approve) ApprovalStatus.APPROVED.name else ApprovalStatus.REJECTED.name
      val batch = db.batch()
      batch.update(
        db.collection("approval_requests").document(request.id),
        mapOf(
          "status" to newStatus,
          "checkerUid" to uid,
          "rejectionReason" to rejectionReason?.takeIf { !approve && it.isNotBlank() },
          "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
      )
      if (request.entityType == "FINANCIAL_POLICY") {
        batch.update(
          db.collection("financial_policies").document(request.entityId),
          mapOf(
            "status" to newStatus,
            "published" to approve,
            "checkerUid" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  /**
   * Issues a time-bound, clinic-restricted single-use healthcare voucher strictly bounded by
   * actual funded Restricted Healthcare Cash.
   */
  suspend fun issueHealthcareVoucher(
    clinic: HealthcareClinic,
    serviceName: String,
    maxAmountUgx: Long,
    availableRestrictedCashUgx: Long
  ): Result<Unit> {
    val uid = requireUserId()
    val voucherId = "vch_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    return try {
      require(maxAmountUgx > 0L) { "Voucher cap must be positive." }
      require(maxAmountUgx <= availableRestrictedCashUgx) {
        "Insufficient funded Restricted Healthcare Cash (Available: UGX $availableRestrictedCashUgx). Unfunded accruals cannot issue vouchers."
      }
      val code = "WH-HC-${voucherId.takeLast(6).uppercase()}"
      val expiresEpochMs = System.currentTimeMillis() + 14L * 86_400_000L
      val viewers = listOf(uid, clinic.ownerUid).distinct()
      db.collection("healthcare_vouchers").document(voucherId).set(
        mapOf(
          "voucherCode" to code,
          "pickerUid" to uid,
          "clinicId" to clinic.id,
          "clinicName" to clinic.clinicName,
          "serviceName" to serviceName.trim().take(100),
          "maxAmountUgxMinor" to maxAmountUgx,
          "currency" to "UGX",
          "expiresAtEpochMs" to expiresEpochMs,
          "status" to VoucherStatus.ISSUED.name,
          "ownerUid" to uid,
          "viewerUids" to viewers,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "healthcare_vouchers/$voucherId")
      Result.failure(e)
    }
  }

  suspend fun redeemVoucherAndSubmitClaim(
    voucher: HealthcareVoucher,
    claimedAmountUgx: Long
  ): Result<Unit> {
    val uid = requireUserId()
    val path = "healthcare_vouchers/${voucher.id}"
    return try {
      require(voucher.status == VoucherStatus.ISSUED.name) { "Voucher already redeemed or expired." }
      require(claimedAmountUgx in 1L..voucher.maxAmountUgxMinor) {
        "Claim amount must be between 1 and ${voucher.maxAmountUgxMinor} UGX."
      }
      val claimId = "clm_${voucher.id}"
      val viewers = (voucher.viewerUids + uid).distinct().take(15)
      val batch = db.batch()
      batch.update(
        db.collection("healthcare_vouchers").document(voucher.id),
        mapOf(
          "status" to VoucherStatus.REDEEMED.name,
          "redeemedByClinicUid" to uid,
          "viewerUids" to viewers,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.set(
        db.collection("healthcare_claims").document(claimId),
        mapOf(
          "voucherId" to voucher.id,
          "clinicId" to voucher.clinicId,
          "clinicOwnerUid" to uid,
          "pickerUid" to voucher.pickerUid,
          "claimedAmountUgxMinor" to claimedAmountUgx,
          "currency" to "UGX",
          "status" to ClaimStatus.SUBMITTED.name,
          "viewerUids" to viewers,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun payApprovedHealthcareClaim(claim: HealthcareClaim, clinicName: String): Result<Unit> {
    val uid = requireUserId()
    val path = "healthcare_claims/${claim.id}"
    return try {
      require(claim.status != ClaimStatus.PAID.name) { "Claim already paid." }
      val entryId = "je_hc_${claim.id}"
      val viewers = (claim.viewerUids + uid).distinct().take(15)
      val journal = DoubleEntryAccountingEngine.forHealthcareClaimPaid(
        entryRef = "JE-HC-${claim.id.takeLast(5).uppercase()}",
        periodId = currentPeriodId(),
        claimId = claim.id,
        hubId = "hub_nakawa",
        amountUgx = claim.claimedAmountUgxMinor,
        clinicName = clinicName
      ).getOrThrow()

      val batch = db.batch()
      batch.update(
        db.collection("healthcare_claims").document(claim.id),
        mapOf(
          "status" to ClaimStatus.PAID.name,
          "reviewedByUid" to uid,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.set(
        db.collection("journal_entries").document(entryId),
        mapOf(
          "entryRef" to journal.entryRef,
          "periodId" to journal.periodId,
          "eventType" to journal.eventType,
          "sourceId" to journal.sourceId,
          "idempotencyKey" to journal.idempotencyKey,
          "description" to journal.description,
          "totalDebitsUgxMinor" to journal.totalDebitsUgxMinor,
          "totalCreditsUgxMinor" to journal.totalCreditsUgxMinor,
          "currency" to "UGX",
          "postedByUid" to uid,
          "status" to "POSTED",
          "viewerUids" to viewers,
          "createdAt" to FieldValue.serverTimestamp()
        )
      )
      journal.lines.forEachIndexed { idx, line ->
        batch.set(
          db.collection("journal_lines").document("${entryId}_$idx"),
          mapOf(
            "entryId" to entryId,
            "accountCode" to line.accountCode,
            "accountName" to line.accountName,
            "accountCategory" to line.accountCategory.name,
            "debitUgxMinor" to line.debitUgxMinor,
            "creditUgxMinor" to line.creditUgxMinor,
            "currency" to "UGX",
            "hubId" to line.hubId,
            "postedByUid" to uid,
            "viewerUids" to viewers,
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun registerHousehold(community: String, hubId: String, memberCount: Long, collectionDay: String): Result<Unit> {
    val uid = requireUserId()
    val hhId = "hh_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    return try {
      db.collection("households").document(hhId).set(
        mapOf(
          "householdRef" to "HH-KLA-${hhId.takeLast(5).uppercase()}",
          "ownerUid" to uid,
          "community" to community.trim().take(100),
          "hubId" to hubId,
          "memberCount" to memberCount.coerceIn(1L, 100L),
          "collectionDay" to collectionDay,
          "pointsBalance" to 10L,
          "contaminationCount" to 0L,
          "viewerUids" to listOf(uid),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "households/$hhId")
      Result.failure(e)
    }
  }

  suspend fun fileDispute(category: String, relatedEntityId: String, description: String): Result<Unit> {
    val uid = requireUserId()
    val dispId = "dsp_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    return try {
      require(description.trim().length >= 4) { "Provide at least 4 characters describing the dispute." }
      db.collection("disputes").document(dispId).set(
        mapOf(
          "disputeRef" to "DSP-${dispId.takeLast(6).uppercase()}",
          "reporterUid" to uid,
          "category" to category.take(40),
          "relatedEntityId" to relatedEntityId.trim().ifBlank { "GENERAL" }.take(128),
          "description" to description.trim().take(500),
          "status" to DisputeStatus.OPEN.name,
          "viewerUids" to listOf(uid),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "disputes/$dispId")
      Result.failure(e)
    }
  }

  suspend fun resolveDispute(dispute: DisputeRecord, resolutionNotes: String): Result<Unit> {
    val uid = requireUserId()
    val path = "disputes/${dispute.id}"
    return try {
      require(dispute.status != DisputeStatus.CLOSED.name) { "Dispute already closed." }
      db.collection("disputes").document(dispute.id).update(
        mapOf(
          "investigatorUid" to uid,
          "status" to DisputeStatus.RESOLVED.name,
          "resolutionNotes" to resolutionNotes.trim().take(500),
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

  private fun rethrowIfCancellation(error: Throwable) {
    val firestoreErr = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreErr == null && error is CancellationException) throw error
  }
}
