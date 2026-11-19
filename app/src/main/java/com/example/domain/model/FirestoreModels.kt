package com.example.domain.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserAccount(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val assignedRoles: List<String> = listOf(UserRole.PICKER.name),
  val activeRole: String = UserRole.PICKER.name,
  val assignedHubId: String? = null,
  val status: String = AccountStatus.ACTIVE.name,
  val preferredLanguage: String = "en",
  val paymentPreference: String = SettlementMethod.DIRECT_MOMO.name,
  val viewerUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserPrivatePii(
  @DocumentId val id: String = "",
  val userId: String = "",
  val email: String? = null,
  val phone: String? = null,
  val nationalIdRef: String? = null,
  val emergencyContact: String? = null,
  val momoDestinationMasked: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CollectionHub(
  @DocumentId val id: String = "",
  val code: String = "",
  val name: String = "",
  val district: String = "",
  val managerUid: String = "",
  val ownerUid: String = "",
  val published: Boolean = true,
  val status: String = "ACTIVE",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WasteMaterial(
  @DocumentId val id: String = "",
  val code: String = "",
  val name: String = "",
  val category: String = MaterialCategory.RECYCLABLE.name,
  val unit: String = "kg",
  val grades: List<String> = listOf("A", "B"),
  val recoveryRoute: String = "",
  val purchaseEligible: Boolean = true,
  val buyerEligible: Boolean = true,
  val co2FactorMilli: Long = 0L,
  val published: Boolean = true,
  val ownerUid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PurchaseRate(
  @DocumentId val id: String = "",
  val materialId: String = "",
  val materialName: String = "",
  val grade: String = "A",
  val hubId: String = "",
  val purchasePriceUgxMinor: Long = 0L,
  val buyerSellPriceUgxMinor: Long = 0L,
  val currency: String = "UGX",
  val version: Long = 1L,
  val status: String = "APPROVED",
  val published: Boolean = true,
  val makerUid: String = "",
  val checkerUid: String? = null,
  val ownerUid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FinancialPolicy(
  @DocumentId val id: String = "",
  val version: Long = 1L,
  val healthcareUgxPerKg: Long = 30L,
  val pickerRewardPct: Long = 30L,
  val operationsPct: Long = 35L,
  val contingencyPct: Long = 20L,
  val expansionPct: Long = 15L,
  val minWorkingCapitalUgx: Long = 2_000_000L,
  val walletEnabled: Boolean = false,
  val status: String = "APPROVED",
  val published: Boolean = true,
  val makerUid: String = "",
  val checkerUid: String? = null,
  val ownerUid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WasteDelivery(
  @DocumentId val id: String = "",
  val idempotencyKey: String = "",
  val originType: String = OriginType.PICKER_HUB.name,
  val hubId: String = "",
  val pickerUid: String = "",
  val pickerName: String = "",
  val householdId: String? = null,
  val agentUid: String = "",
  val materialId: String = "",
  val materialName: String = "",
  val grade: String = "A",
  val grossKgMilli: Long = 0L,
  val tareKgMilli: Long = 0L,
  val netKgMilli: Long = 0L,
  val deductionKgMilli: Long = 0L,
  val acceptedKgMilli: Long = 0L,
  val rateIdSnapshot: String = "",
  val unitPriceUgxMinor: Long = 0L,
  val totalEarningsUgxMinor: Long = 0L,
  val currency: String = "UGX",
  val receiptRef: String = "",
  val evidenceUri: String? = null,
  val verificationStatus: String = VerificationStatus.PENDING_VERIFICATION.name,
  val originalEventEpochMs: Long = 0L,
  val deviceId: String = "",
  val viewerUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class InventoryBatch(
  @DocumentId val id: String = "",
  val batchCode: String = "",
  val hubId: String = "",
  val materialId: String = "",
  val materialName: String = "",
  val grade: String = "A",
  val sourceDeliveryId: String = "",
  val receivedKgMilli: Long = 0L,
  val availableKgMilli: Long = 0L,
  val allocatedKgMilli: Long = 0L,
  val soldKgMilli: Long = 0L,
  val rejectedKgMilli: Long = 0L,
  val disposedKgMilli: Long = 0L,
  val unitCostUgxMinor: Long = 0L,
  val status: String = "RECEIVED",
  val ownerUid: String = "",
  val viewerUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CompostBatch(
  @DocumentId val id: String = "",
  val batchCode: String = "",
  val hubId: String = "",
  val inputKgMilli: Long = 0L,
  val materialComposition: String = "",
  val moisturePctMilli: Long = 55000L,
  val temperatureCelsiusMilli: Long = 55000L,
  val turningCount: Long = 0L,
  val rejectedKgMilli: Long = 0L,
  val lossKgMilli: Long = 0L,
  val outputCompostKgMilli: Long = 0L,
  val soldCompostKgMilli: Long = 0L,
  val processingCostUgxMinor: Long = 0L,
  val status: String = CompostStatus.ACTIVE.name,
  val operatorUid: String = "",
  val approvedByUid: String? = null,
  val viewerUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WasteSale(
  @DocumentId val id: String = "",
  val saleRef: String = "",
  val buyerName: String = "",
  val buyerOwnerUid: String = "",
  val hubId: String = "",
  val batchId: String = "",
  val materialId: String = "",
  val materialName: String = "",
  val grade: String = "A",
  val dispatchedKgMilli: Long = 0L,
  val confirmedSoldKgMilli: Long = 0L,
  val rejectedKgMilli: Long = 0L,
  val rejectionReason: String? = null,
  val unitSellPriceUgxMinor: Long = 0L,
  val totalInvoiceUgxMinor: Long = 0L,
  val paidUgxMinor: Long = 0L,
  val currency: String = "UGX",
  val healthcareRateUgxPerKg: Long = 30L,
  val healthcareAccruedUgxMinor: Long = 0L,
  val healthcareFundedUgxMinor: Long = 0L,
  val status: String = SaleStatus.ORDERED.name,
  val ownerUid: String = "",
  val viewerUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class JournalEntry(
  @DocumentId val id: String = "",
  val entryRef: String = "",
  val periodId: String = "",
  val eventType: String = "",
  val sourceId: String = "",
  val idempotencyKey: String = "",
  val description: String = "",
  val totalDebitsUgxMinor: Long = 0L,
  val totalCreditsUgxMinor: Long = 0L,
  val currency: String = "UGX",
  val postedByUid: String = "",
  val reversedEntryId: String? = null,
  val status: String = "POSTED",
  val viewerUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class JournalLine(
  @DocumentId val id: String = "",
  val entryId: String = "",
  val accountCode: String = "",
  val accountName: String = "",
  val accountCategory: String = AccountCategory.ASSET.name,
  val debitUgxMinor: Long = 0L,
  val creditUgxMinor: Long = 0L,
  val currency: String = "UGX",
  val hubId: String = "",
  val postedByUid: String = "",
  val viewerUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PickerEarning(
  @DocumentId val id: String = "",
  val deliveryId: String = "",
  val pickerUid: String = "",
  val pickerName: String = "",
  val hubId: String = "",
  val acceptedKgMilli: Long = 0L,
  val unitRateUgxMinor: Long = 0L,
  val amountUgxMinor: Long = 0L,
  val currency: String = "UGX",
  val settlementMethod: String = SettlementMethod.DIRECT_MOMO.name,
  val payoutStatus: String = PayoutStatus.UNSETTLED.name,
  val payoutId: String? = null,
  val ownerUid: String = "",
  val viewerUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PickerPayout(
  @DocumentId val id: String = "",
  val idempotencyKey: String = "",
  val earningId: String = "",
  val pickerUid: String = "",
  val pickerName: String = "",
  val provider: String = MoMoProvider.MTN_MOMO.name,
  val destinationMasked: String = "",
  val amountUgxMinor: Long = 0L,
  val currency: String = "UGX",
  val lifecycleStatus: String = MoMoLifecycleStatus.CREATED.name,
  val providerRef: String? = null,
  val failureCode: String? = null,
  val failureReason: String? = null,
  val reconciliationStatus: String = ReconciliationStatus.UNRECONCILED.name,
  val makerUid: String = "",
  val checkerUid: String? = null,
  val viewerUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HealthcareClinic(
  @DocumentId val id: String = "",
  val clinicName: String = "",
  val location: String = "",
  val licenseRef: String = "",
  val servicesSummary: String = "",
  val ownerUid: String = "",
  val published: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HealthcareVoucher(
  @DocumentId val id: String = "",
  val voucherCode: String = "",
  val pickerUid: String = "",
  val clinicId: String = "",
  val clinicName: String = "",
  val serviceName: String = "",
  val maxAmountUgxMinor: Long = 0L,
  val currency: String = "UGX",
  val expiresAtEpochMs: Long = 0L,
  val status: String = VoucherStatus.ISSUED.name,
  val redeemedByClinicUid: String? = null,
  val ownerUid: String = "",
  val viewerUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HealthcareClaim(
  @DocumentId val id: String = "",
  val voucherId: String = "",
  val clinicId: String = "",
  val clinicOwnerUid: String = "",
  val pickerUid: String = "",
  val claimedAmountUgxMinor: Long = 0L,
  val currency: String = "UGX",
  val status: String = ClaimStatus.SUBMITTED.name,
  val reviewedByUid: String? = null,
  val viewerUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Household(
  @DocumentId val id: String = "",
  val householdRef: String = "",
  val ownerUid: String = "",
  val community: String = "",
  val hubId: String = "",
  val memberCount: Long = 1L,
  val collectionDay: String = "Monday",
  val pointsBalance: Long = 0L,
  val contaminationCount: Long = 0L,
  val viewerUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DisputeRecord(
  @DocumentId val id: String = "",
  val disputeRef: String = "",
  val reporterUid: String = "",
  val category: String = "WEIGHING",
  val relatedEntityId: String = "",
  val description: String = "",
  val investigatorUid: String? = null,
  val status: String = DisputeStatus.OPEN.name,
  val resolutionNotes: String? = null,
  val viewerUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ApprovalRequest(
  @DocumentId val id: String = "",
  val entityType: String = "",
  val entityId: String = "",
  val actionSummary: String = "",
  val payloadSummary: String = "",
  val makerUid: String = "",
  val checkerUid: String? = null,
  val status: String = ApprovalStatus.PENDING.name,
  val rejectionReason: String? = null,
  val viewerUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLog(
  @DocumentId val id: String = "",
  val actorUid: String = "",
  val action: String = "",
  val entityType: String = "",
  val entityId: String = "",
  val summary: String = "",
  val viewerUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null
)
