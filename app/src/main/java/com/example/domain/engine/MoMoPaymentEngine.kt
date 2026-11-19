package com.example.domain.engine

import com.example.domain.model.MoMoLifecycleStatus
import com.example.domain.model.MoMoProvider
import com.example.domain.model.ReconciliationStatus
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class SandboxScenario(val label: String) {
  IMMEDIATE_SUCCESS("Sandbox: Verified Settlement (SUCCESS)"),
  NETWORK_TIMEOUT("Sandbox: Gateway Timeout -> UNKNOWN Lock"),
  DUPLICATE_CALLBACK("Sandbox: Duplicate Webhook Idempotency Test"),
  OUT_OF_ORDER_CALLBACK("Sandbox: Out-of-Order Callback Guard"),
  INSUFFICIENT_FLOAT("Sandbox: Insufficient Provider Balance (FAILED)"),
  INVALID_DESTINATION("Sandbox: Unregistered MSISDN (FAILED)")
}

data class MoMoDisbursementRequest(
  val internalPayoutId: String,
  val idempotencyKey: String,
  val provider: MoMoProvider,
  val destinationMsisdnMasked: String,
  val amountUgxMinor: Long,
  val currency: String = "UGX"
)

data class MoMoCallbackPayload(
  val providerRef: String,
  val idempotencyKey: String,
  val amountUgxMinor: Long,
  val currency: String,
  val destinationMasked: String,
  val providerStatus: String,
  val signatureHex: String
)

data class MoMoExecutionOutcome(
  val nextLifecycleStatus: MoMoLifecycleStatus,
  val reconciliationStatus: ReconciliationStatus,
  val providerRef: String?,
  val failureCode: String?,
  val failureReason: String?,
  val canRetryAutomatically: Boolean
)

/**
 * Payment orchestration state machine and provider adapter interface.
 * Enforces:
 * - CREATED -> VALIDATING -> SUBMITTED -> PENDING -> SUCCESS / FAILED / UNKNOWN
 * - Timeouts NEVER auto-fail or auto-retry; they transition to UNKNOWN + UNRESOLVED for reconciliation.
 * - Callbacks require HMAC signature, reference, amount, currency, and destination verification.
 */
object MoMoPaymentEngine {

  private val ALLOWED_TRANSITIONS: Map<MoMoLifecycleStatus, Set<MoMoLifecycleStatus>> = mapOf(
    MoMoLifecycleStatus.CREATED to setOf(MoMoLifecycleStatus.VALIDATING, MoMoLifecycleStatus.FAILED),
    MoMoLifecycleStatus.VALIDATING to setOf(MoMoLifecycleStatus.SUBMITTED, MoMoLifecycleStatus.FAILED),
    MoMoLifecycleStatus.SUBMITTED to setOf(
      MoMoLifecycleStatus.PENDING,
      MoMoLifecycleStatus.SUCCESS,
      MoMoLifecycleStatus.FAILED,
      MoMoLifecycleStatus.UNKNOWN
    ),
    MoMoLifecycleStatus.PENDING to setOf(
      MoMoLifecycleStatus.SUCCESS,
      MoMoLifecycleStatus.FAILED,
      MoMoLifecycleStatus.UNKNOWN
    ),
    MoMoLifecycleStatus.UNKNOWN to setOf(
      MoMoLifecycleStatus.SUCCESS,
      MoMoLifecycleStatus.FAILED
    ),
    MoMoLifecycleStatus.FAILED to emptySet(),
    MoMoLifecycleStatus.SUCCESS to emptySet()
  )

  fun canTransition(from: MoMoLifecycleStatus, to: MoMoLifecycleStatus): Boolean =
    ALLOWED_TRANSITIONS[from]?.contains(to) == true

  fun simulateSandboxDisbursement(
    request: MoMoDisbursementRequest,
    scenario: SandboxScenario
  ): MoMoExecutionOutcome {
    if (request.amountUgxMinor <= 0L || request.currency != "UGX") {
      return MoMoExecutionOutcome(
        nextLifecycleStatus = MoMoLifecycleStatus.FAILED,
        reconciliationStatus = ReconciliationStatus.UNRECONCILED,
        providerRef = null,
        failureCode = "INVALID_REQUEST",
        failureReason = "Positive UGX amount required.",
        canRetryAutomatically = false
      )
    }

    val prefix = if (request.provider == MoMoProvider.MTN_MOMO) "MTN-SBX" else "AIR-SBX"
    val ref = "$prefix-${request.internalPayoutId.takeLast(8).uppercase()}"

    return when (scenario) {
      SandboxScenario.IMMEDIATE_SUCCESS -> MoMoExecutionOutcome(
        nextLifecycleStatus = MoMoLifecycleStatus.SUCCESS,
        reconciliationStatus = ReconciliationStatus.MATCHED,
        providerRef = ref,
        failureCode = null,
        failureReason = null,
        canRetryAutomatically = false
      )
      SandboxScenario.NETWORK_TIMEOUT -> MoMoExecutionOutcome(
        nextLifecycleStatus = MoMoLifecycleStatus.UNKNOWN,
        reconciliationStatus = ReconciliationStatus.UNRESOLVED,
        providerRef = ref,
        failureCode = "GATEWAY_TIMEOUT",
        failureReason = "Timeout after submission. Locked in UNKNOWN/PENDING_RECONCILIATION — query provider by $ref before any retry.",
        canRetryAutomatically = false
      )
      SandboxScenario.DUPLICATE_CALLBACK -> MoMoExecutionOutcome(
        nextLifecycleStatus = MoMoLifecycleStatus.SUCCESS,
        reconciliationStatus = ReconciliationStatus.MATCHED,
        providerRef = ref,
        failureCode = "DUP_IGNORED",
        failureReason = "Duplicate callback for idempotencyKey=${request.idempotencyKey} deduplicated safely.",
        canRetryAutomatically = false
      )
      SandboxScenario.OUT_OF_ORDER_CALLBACK -> MoMoExecutionOutcome(
        nextLifecycleStatus = MoMoLifecycleStatus.PENDING,
        reconciliationStatus = ReconciliationStatus.UNRECONCILED,
        providerRef = ref,
        failureCode = "OUT_OF_ORDER",
        failureReason = "Pending status kept awaiting authoritative final callback.",
        canRetryAutomatically = false
      )
      SandboxScenario.INSUFFICIENT_FLOAT -> MoMoExecutionOutcome(
        nextLifecycleStatus = MoMoLifecycleStatus.FAILED,
        reconciliationStatus = ReconciliationStatus.MATCHED,
        providerRef = ref,
        failureCode = "LOW_FLOAT",
        failureReason = "Insufficient Mobile Money disbursement float.",
        canRetryAutomatically = false
      )
      SandboxScenario.INVALID_DESTINATION -> MoMoExecutionOutcome(
        nextLifecycleStatus = MoMoLifecycleStatus.FAILED,
        reconciliationStatus = ReconciliationStatus.MATCHED,
        providerRef = ref,
        failureCode = "INVALID_MSISDN",
        failureReason = "Destination account is not registered for mobile money.",
        canRetryAutomatically = false
      )
    }
  }

  fun computeWebhookHmacHex(payloadBody: String, secret: String): String {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
    return mac.doFinal(payloadBody.toByteArray(Charsets.UTF_8))
      .joinToString("") { "%02x".format(it) }
  }

  fun verifyCallback(
    expected: MoMoDisbursementRequest,
    currentStatus: MoMoLifecycleStatus,
    callback: MoMoCallbackPayload,
    webhookSecret: String,
    alreadyProcessedRef: Boolean
  ): Result<MoMoLifecycleStatus> {
    if (alreadyProcessedRef && currentStatus == MoMoLifecycleStatus.SUCCESS) {
      return Result.failure(IllegalStateException("Duplicate callback ignored: payout already settled."))
    }
    val canonical = "${callback.providerRef}|${callback.idempotencyKey}|${callback.amountUgxMinor}|${callback.currency}|${callback.providerStatus}"
    val expectedSig = computeWebhookHmacHex(canonical, webhookSecret)
    val sigValid = MessageDigest.isEqual(
      expectedSig.toByteArray(Charsets.UTF_8),
      callback.signatureHex.lowercase().toByteArray(Charsets.UTF_8)
    )
    if (!sigValid) {
      return Result.failure(SecurityException("Webhook signature verification failed."))
    }
    if (callback.idempotencyKey != expected.idempotencyKey ||
      callback.amountUgxMinor != expected.amountUgxMinor ||
      callback.currency != expected.currency ||
      callback.destinationMasked != expected.destinationMsisdnMasked
    ) {
      return Result.failure(SecurityException("Webhook payload mismatch against internal payout record."))
    }
    val targetStatus = when (callback.providerStatus.uppercase()) {
      "SUCCESSFUL", "SUCCESS" -> MoMoLifecycleStatus.SUCCESS
      "FAILED", "REJECTED" -> MoMoLifecycleStatus.FAILED
      "PENDING" -> MoMoLifecycleStatus.PENDING
      else -> MoMoLifecycleStatus.UNKNOWN
    }
    if (!canTransition(currentStatus, targetStatus)) {
      return Result.failure(
        IllegalStateException("Out-of-order transition blocked: $currentStatus -> $targetStatus")
      )
    }
    return Result.success(targetStatus)
  }
}
