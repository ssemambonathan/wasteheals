package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.OfflineCollectionEntity
import com.example.data.local.ScaleCalibrationEntity
import com.example.data.remote.WasteHealsCatalogRepository
import com.example.data.remote.WasteHealsFinanceHealthRepository
import com.example.data.remote.WasteHealsOperationsRepository
import com.example.domain.engine.DoubleEntryAccountingEngine
import com.example.domain.engine.DraftJournalLine
import com.example.domain.engine.FinancialStatementsSummary
import com.example.domain.engine.MoneyAndWeight
import com.example.domain.engine.PickerRewardAllocation
import com.example.domain.engine.PickerScoreInput
import com.example.domain.engine.SandboxScenario
import com.example.domain.engine.SurplusAllocationResult
import com.example.domain.engine.SurplusAndLiquidityEngine
import com.example.domain.engine.SurplusInput
import com.example.domain.engine.SurplusPolicyConfig
import com.example.domain.model.AccountCategory
import com.example.domain.model.ApprovalRequest
import com.example.domain.model.AuditLog
import com.example.domain.model.CollectionHub
import com.example.domain.model.CompostBatch
import com.example.domain.model.DisputeRecord
import com.example.domain.model.FinancialPolicy
import com.example.domain.model.HealthcareClaim
import com.example.domain.model.HealthcareClinic
import com.example.domain.model.HealthcareVoucher
import com.example.domain.model.Household
import com.example.domain.model.InventoryBatch
import com.example.domain.model.JournalEntry
import com.example.domain.model.JournalLine
import com.example.domain.model.MoMoProvider
import com.example.domain.model.OriginType
import com.example.domain.model.PayoutStatus
import com.example.domain.model.PickerEarning
import com.example.domain.model.PickerPayout
import com.example.domain.model.PurchaseRate
import com.example.domain.model.UserAccount
import com.example.domain.model.UserPrivatePii
import com.example.domain.model.UserRole
import com.example.domain.model.UserRoleKey
import com.example.domain.model.VerificationStatus
import com.example.domain.model.WasteDelivery
import com.example.domain.model.WasteMaterial
import com.example.domain.model.WasteSale
import com.example.ui.auth.PendingAuthOnboarding
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

data class WasteHealsSnapshot(
  val profile: UserAccount? = null,
  val pii: UserPrivatePii? = null,
  val hubs: List<CollectionHub> = emptyList(),
  val materials: List<WasteMaterial> = emptyList(),
  val rates: List<PurchaseRate> = emptyList(),
  val policy: FinancialPolicy = FinancialPolicy(),
  val clinics: List<HealthcareClinic> = emptyList(),
  val offlineQueue: List<OfflineCollectionEntity> = emptyList(),
  val scales: List<ScaleCalibrationEntity> = emptyList(),
  val deliveries: List<WasteDelivery> = emptyList(),
  val inventory: List<InventoryBatch> = emptyList(),
  val compostBatches: List<CompostBatch> = emptyList(),
  val sales: List<WasteSale> = emptyList(),
  val journalEntries: List<JournalEntry> = emptyList(),
  val journalLines: List<JournalLine> = emptyList(),
  val earnings: List<PickerEarning> = emptyList(),
  val payouts: List<PickerPayout> = emptyList(),
  val vouchers: List<HealthcareVoucher> = emptyList(),
  val claims: List<HealthcareClaim> = emptyList(),
  val households: List<Household> = emptyList(),
  val disputes: List<DisputeRecord> = emptyList(),
  val approvals: List<ApprovalRequest> = emptyList(),
  val auditLogs: List<AuditLog> = emptyList(),
  val allUsers: List<UserAccount> = emptyList()
) {
  val activeRole: UserRole get() = UserRole.fromString(profile?.activeRole)

  val assignedRoles: List<UserRole>
    get() = (profile?.assignedRoles ?: emptyList()).map { UserRole.fromString(it) }.distinct()
      .ifEmpty { listOf(UserRole.PICKER) }

  val ledgerSummary: FinancialStatementsSummary by lazy {
    DoubleEntryAccountingEngine.summarizeLedger(
      journalLines.map {
        DraftJournalLine(
          accountCode = it.accountCode,
          accountName = it.accountName,
          accountCategory = AccountCategory.fromString(it.accountCategory),
          debitUgxMinor = it.debitUgxMinor,
          creditUgxMinor = it.creditUgxMinor,
          hubId = it.hubId
        )
      }
    )
  }

  val surplusEvaluation: SurplusAllocationResult by lazy {
    val led = ledgerSummary
    val dueHc = sales.sumOf { (it.healthcareAccruedUgxMinor - it.healthcareFundedUgxMinor).coerceAtLeast(0L) }
    val transportAndOps = compostBatches.sumOf { it.processingCostUgxMinor }
    val policyCfg = SurplusPolicyConfig(
      version = policy.version.toInt(),
      healthcareUgxPerKg = policy.healthcareUgxPerKg,
      pickerRewardPct = policy.pickerRewardPct.toInt(),
      operationsPct = policy.operationsPct.toInt(),
      contingencyPct = policy.contingencyPct.toInt(),
      expansionPct = policy.expansionPct.toInt(),
      minWorkingCapitalUgx = policy.minWorkingCapitalUgx
    )
    SurplusAndLiquidityEngine.evaluateSurplusAndLiquidity(
      input = SurplusInput(
        revenueUgx = led.salesRevenueUgx,
        cogsUgx = led.totalExpensesAndCogsUgx,
        transportAndSortingUgx = transportAndOps,
        healthcareContributionUgx = 0L, // already included in ledger expenses
        operatingExpensesUgx = 0L,
        rejectionAndLossReserveUgx = sales.sumOf {
          MoneyAndWeight.calculateAmountUgx(it.rejectedKgMilli, it.unitSellPriceUgxMinor)
        },
        taxesAndObligationsUgx = 0L,
        operatingCashOnHandUgx = led.operatingCashUgx,
        unsettledPickerPayablesUgx = led.pickerEarningsPayableUgx,
        dueHealthcareCashTransferUgx = dueHc,
        dueOperatingPayablesUgx = 0L,
        contingencyLiquidityReserveUgx = 500_000L
      ),
      policy = policyCfg
    )
  }

  val rewardAllocations: List<PickerRewardAllocation> by lazy {
    val pool = surplusEvaluation.pickerRewardPoolUgx
    val grouped = deliveries
      .filter { it.verificationStatus == VerificationStatus.ACCEPTED.name }
      .groupBy { it.pickerUid }
      .map { (pUid, list) ->
        PickerScoreInput(
          pickerUid = pUid,
          pickerName = list.first().pickerName,
          acceptedKgMilli = list.sumOf { it.acceptedKgMilli },
          qualityAccuracyPct = 94,
          consistencyPct = (list.size * 25).coerceIn(50, 100),
          safetyCompliancePct = 100
        )
      }
    SurplusAndLiquidityEngine.distributePickerRewards(pool, grouped)
  }

  val totalCollectedKgMilli: Long get() = deliveries.sumOf { it.netKgMilli }
  val totalAcceptedKgMilli: Long
    get() = deliveries.filter { it.verificationStatus == VerificationStatus.ACCEPTED.name }
      .sumOf { it.acceptedKgMilli }
  val totalConfirmedSoldKgMilli: Long get() = sales.sumOf { it.confirmedSoldKgMilli }
  val totalCo2AvoidedKgMilli: Long
    get() {
      val factorByMat = materials.associate { it.id to it.co2FactorMilli }
      return sales.sumOf { sale ->
        val factor = factorByMat[sale.materialId] ?: 1000L
        (sale.confirmedSoldKgMilli * factor) / 1000L
      }
    }
}

class WasteHealsViewModel(
  private val catalogRepo: WasteHealsCatalogRepository,
  private val opsRepo: WasteHealsOperationsRepository,
  private val finRepo: WasteHealsFinanceHealthRepository,
  private val currentUserId: String
) : ViewModel() {

  private val _feedback = MutableStateFlow<String?>(null)
  val feedback: StateFlow<String?> = _feedback.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  init {
    viewModelScope.launch {
      val (pendingName, pendingRole) = PendingAuthOnboarding.consume()
      catalogRepo.ensureUserOnboarded(
        preferredDisplayName = pendingName,
        requestedRoleKey = pendingRole
      ).onFailure { _feedback.value = "Onboarding warning: ${it.localizedMessage}" }
    }
  }

  private fun <T> Flow<T>.safeStream(label: String): Flow<T> = catch { error ->
    val firestoreErr = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreErr == null && error is CancellationException) throw error
    Log.w("WasteHealsVM", "Stream error in $label", firestoreErr ?: error)
    throw (firestoreErr ?: error)
  }

  private val catalogPart = combine(
    catalogRepo.observeUserProfile(currentUserId).safeStream("profile"),
    catalogRepo.observeUserPrivatePii(currentUserId).safeStream("pii"),
    catalogRepo.observePublishedHubs().safeStream("hubs"),
    catalogRepo.observePublishedMaterials().safeStream("materials"),
    catalogRepo.observeApprovedRates().safeStream("rates")
  ) { profile, pii, hubs, mats, rates ->
    CatalogBundle(profile, pii, hubs, mats, rates)
  }

  private val opsPart = combine(
    catalogRepo.observeActivePolicy().safeStream("policy"),
    catalogRepo.observePublishedClinics().safeStream("clinics"),
    opsRepo.observeOfflineQueue(),
    catalogRepo.observeScaleCalibrations(),
    opsRepo.observeDeliveries(currentUserId).safeStream("deliveries")
  ) { policy, clinics, queue, scales, deliveries ->
    OpsBundleA(policy ?: FinancialPolicy(), clinics, queue, scales, deliveries)
  }

  private val opsPartB = combine(
    opsRepo.observeInventoryBatches(currentUserId).safeStream("inventory"),
    opsRepo.observeCompostBatches(currentUserId).safeStream("compost"),
    opsRepo.observeWasteSales(currentUserId).safeStream("sales"),
    finRepo.observeJournalEntries(currentUserId).safeStream("je"),
    finRepo.observeJournalLines(currentUserId).safeStream("jl")
  ) { inv, comp, sales, je, jl ->
    OpsBundleB(inv, comp, sales, je, jl)
  }

  private val finPartA = combine(
    finRepo.observePickerEarnings(currentUserId).safeStream("earnings"),
    finRepo.observePickerPayouts(currentUserId).safeStream("payouts"),
    finRepo.observeVouchers(currentUserId).safeStream("vouchers"),
    finRepo.observeClaims(currentUserId).safeStream("claims")
  ) { earn, pay, vch, clm ->
    FinBundleA(earn, pay, vch, clm)
  }

  private val finPartB = combine(
    finRepo.observeHouseholds(currentUserId).safeStream("households"),
    finRepo.observeDisputes(currentUserId).safeStream("disputes"),
    finRepo.observeApprovalRequests(currentUserId).safeStream("approvals"),
    finRepo.observeAuditLogs(currentUserId).safeStream("audits"),
    catalogRepo.observeAllUsers(currentUserId).safeStream("users")
  ) { hh, dsp, apr, aud, users ->
    FinBundleB(hh, dsp, apr, aud, users)
  }

  val uiState: StateFlow<UiState<WasteHealsSnapshot>> = combine(
    catalogPart, opsPart, opsPartB, finPartA, finPartB
  ) { c, oa, ob, fa, fb ->
    WasteHealsSnapshot(
      profile = c.profile,
      pii = c.pii,
      hubs = c.hubs,
      materials = c.materials,
      rates = c.rates,
      policy = oa.policy,
      clinics = oa.clinics,
      offlineQueue = oa.queue,
      scales = oa.scales,
      deliveries = oa.deliveries,
      inventory = ob.inventory,
      compostBatches = ob.compost,
      sales = ob.sales,
      journalEntries = ob.journalEntries,
      journalLines = ob.journalLines,
      earnings = fa.earnings,
      payouts = fa.payouts,
      vouchers = fa.vouchers,
      claims = fa.claims,
      households = fb.households,
      disputes = fb.disputes,
      approvals = fb.approvals,
      auditLogs = fb.audits,
      allUsers = fb.users
    )
  }
    .map<WasteHealsSnapshot, UiState<WasteHealsSnapshot>> { UiState.Success(it) }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val root = firestoreError ?: error
      emit(UiState.Error(root.message ?: "Failed to load WasteHeals operational state."))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = UiState.Loading
    )

  fun clearFeedback() { _feedback.value = null }

  private fun runAction(successMessage: String, block: suspend () -> Result<Any?>) {
    viewModelScope.launch {
      _isBusy.value = true
      block()
        .onSuccess { _feedback.value = successMessage }
        .onFailure { _feedback.value = it.localizedMessage ?: "Operation failed." }
      _isBusy.value = false
    }
  }

  fun switchRole(role: UserRole) = runAction("Switched workspace to ${role.title}") {
    catalogRepo.switchActiveRole(role)
  }

  fun assignUserRoles(targetUid: String, roleKeys: List<UserRoleKey>) =
    runAction("Updated Firestore RBAC roles for user") {
      catalogRepo.assignUserRoles(targetUid, roleKeys)
    }

  fun enqueueOfflineCollection(
    originType: OriginType,
    hubId: String,
    pickerName: String,
    householdId: String?,
    rate: PurchaseRate,
    grossKgMilli: Long,
    tareKgMilli: Long,
    deductionKgMilli: Long,
    evidenceUri: String?
  ) = runAction("Saved provisional receipt to offline Room queue") {
    opsRepo.enqueueOfflineCollection(
      originType = originType,
      hubId = hubId,
      pickerUid = currentUserId,
      pickerName = pickerName,
      householdId = householdId,
      rate = rate,
      grossKgMilli = grossKgMilli,
      tareKgMilli = tareKgMilli,
      deductionKgMilli = deductionKgMilli,
      evidenceUri = evidenceUri
    )
  }

  fun syncOfflineQueue() = viewModelScope.launch {
    _isBusy.value = true
    opsRepo.syncPendingOfflineCollections()
      .onSuccess { count -> _feedback.value = "Synced $count collection event(s) idempotently." }
      .onFailure { _feedback.value = it.localizedMessage ?: "Sync error." }
    _isBusy.value = false
  }

  fun acceptDelivery(delivery: WasteDelivery) = runAction("Accepted ${delivery.receiptRef} & posted balanced ledger entry") {
    opsRepo.acceptDeliveryAtomically(delivery)
  }

  fun createAndConfirmSale(
    batchItem: InventoryBatch,
    buyerName: String,
    dispatchedKgMilli: Long,
    confirmedSoldKgMilli: Long,
    rejectedKgMilli: Long,
    rejectionReason: String?,
    unitSellPriceUgx: Long,
    healthcareUgxPerKg: Long
  ) = runAction("Sale confirmed & UGX $healthcareUgxPerKg/kg healthcare obligation accrued") {
    opsRepo.createAndConfirmBuyerSale(
      batchItem, buyerName, dispatchedKgMilli, confirmedSoldKgMilli,
      rejectedKgMilli, rejectionReason, unitSellPriceUgx, healthcareUgxPerKg
    )
  }

  fun recordBuyerReceipt(sale: WasteSale, cashUgx: Long) =
    runAction("Buyer receipt posted & restricted healthcare cash funded") {
      opsRepo.recordBuyerCashReceipt(sale, cashUgx)
    }

  fun createCompostBatch(hubId: String, inputKgMilli: Long, composition: String, costUgx: Long) =
    runAction("Started organic composting batch") {
      opsRepo.createCompostBatch(hubId, inputKgMilli, composition, costUgx)
    }

  fun updateCompostBatch(
    batch: CompostBatch,
    moisturePctMilli: Long,
    tempMilli: Long,
    outputKgMilli: Long,
    rejectedKgMilli: Long,
    approveFinished: Boolean
  ) = runAction("Recorded compost inspection & mass balance") {
    opsRepo.logCompostTurnAndOutput(batch, moisturePctMilli, tempMilli, outputKgMilli, rejectedKgMilli, approveFinished)
  }

  fun initiatePayout(earning: PickerEarning, provider: MoMoProvider, destMasked: String) =
    runAction("Queued MoMo payout draft for Maker-Checker review") {
      finRepo.createPayoutDraft(earning, provider, destMasked)
    }

  fun executeSandboxPayout(payout: PickerPayout, scenario: SandboxScenario) =
    runAction("Sandbox execution: ${scenario.label}") {
      finRepo.executeSandboxPayout(payout, scenario)
    }

  fun reconcileUnknownPayout(payout: PickerPayout, confirmedSuccess: Boolean) =
    runAction("Reconciliation updated for ${payout.id}") {
      finRepo.reconcileUnknownPayout(payout, confirmedSuccess)
    }

  fun proposePolicy(
    currentVersion: Long,
    hcUgx: Long,
    rewardPct: Long,
    opsPct: Long,
    contPct: Long,
    expPct: Long,
    minWcUgx: Long
  ) = runAction("Submitted Policy v${currentVersion + 1} for Checker approval") {
    catalogRepo.proposeFinancialPolicyUpdate(currentVersion, hcUgx, rewardPct, opsPct, contPct, expPct, minWcUgx)
  }

  fun resolveApproval(req: ApprovalRequest, approve: Boolean, reason: String?) =
    runAction(if (approve) "Approval signed by Checker" else "Request rejected") {
      finRepo.approveOrRejectRequest(req, approve, reason)
    }

  fun issueVoucher(clinic: HealthcareClinic, serviceName: String, maxUgx: Long, availableFundedUgx: Long) =
    runAction("Issued single-use clinic voucher") {
      finRepo.issueHealthcareVoucher(clinic, serviceName, maxUgx, availableFundedUgx)
    }

  fun redeemVoucher(voucher: HealthcareVoucher, claimUgx: Long) =
    runAction("Redeemed voucher ${voucher.voucherCode} & submitted clinic claim") {
      finRepo.redeemVoucherAndSubmitClaim(voucher, claimUgx)
    }

  fun payClaim(claim: HealthcareClaim, clinicName: String) =
    runAction("Settled clinic claim from Restricted Healthcare Fund") {
      finRepo.payApprovedHealthcareClaim(claim, clinicName)
    }

  fun registerHousehold(community: String, hubId: String, members: Long, day: String) =
    runAction("Registered household with separation schedule") {
      finRepo.registerHousehold(community, hubId, members, day)
    }

  fun fileDispute(category: String, refId: String, desc: String) =
    runAction("Filed formal dispute for investigation") {
      finRepo.fileDispute(category, refId, desc)
    }

  fun resolveDispute(dispute: DisputeRecord, notes: String) =
    runAction("Dispute ${dispute.disputeRef} resolved with audit trail") {
      finRepo.resolveDispute(dispute, notes)
    }

  fun buildCsvComplianceReport(snap: WasteHealsSnapshot): String {
    val sb = StringBuilder()
    sb.appendLine("WasteHeals Audit & Impact Export (Currency: UGX)")
    sb.appendLine("Metric,Value")
    sb.appendLine("Total Collected Kg,${MoneyAndWeight.milliKgToBigDecimal(snap.totalCollectedKgMilli)}")
    sb.appendLine("Total Accepted Kg,${MoneyAndWeight.milliKgToBigDecimal(snap.totalAcceptedKgMilli)}")
    sb.appendLine("Total Confirmed Sold Kg,${MoneyAndWeight.milliKgToBigDecimal(snap.totalConfirmedSoldKgMilli)}")
    sb.appendLine("CO2 Avoided Kg,${MoneyAndWeight.milliKgToBigDecimal(snap.totalCo2AvoidedKgMilli)}")
    sb.appendLine("Operating Cash UGX,${snap.ledgerSummary.operatingCashUgx}")
    sb.appendLine("Restricted Healthcare Cash Funded UGX,${snap.ledgerSummary.restrictedHealthcareCashUgx}")
    sb.appendLine("Healthcare Accrued Unfunded UGX,${snap.ledgerSummary.healthcareAccruedUnfundedUgx}")
    sb.appendLine("Picker Earnings Payable UGX,${snap.ledgerSummary.pickerEarningsPayableUgx}")
    sb.appendLine("Trial Balance Balanced,${snap.ledgerSummary.isBalanced}")
    sb.appendLine()
    sb.appendLine("AccountCode,AccountName,Category,DebitsUGX,CreditsUGX")
    snap.ledgerSummary.trialBalance.forEach { row ->
      sb.appendLine("${row.accountCode},\"${row.accountName}\",${row.category.name},${row.totalDebitsUgx},${row.totalCreditsUgx}")
    }
    return sb.toString()
  }

  private data class CatalogBundle(
    val profile: UserAccount?,
    val pii: UserPrivatePii?,
    val hubs: List<CollectionHub>,
    val materials: List<WasteMaterial>,
    val rates: List<PurchaseRate>
  )

  private data class OpsBundleA(
    val policy: FinancialPolicy,
    val clinics: List<HealthcareClinic>,
    val queue: List<OfflineCollectionEntity>,
    val scales: List<ScaleCalibrationEntity>,
    val deliveries: List<WasteDelivery>
  )

  private data class OpsBundleB(
    val inventory: List<InventoryBatch>,
    val compost: List<CompostBatch>,
    val sales: List<WasteSale>,
    val journalEntries: List<JournalEntry>,
    val journalLines: List<JournalLine>
  )

  private data class FinBundleA(
    val earnings: List<PickerEarning>,
    val payouts: List<PickerPayout>,
    val vouchers: List<HealthcareVoucher>,
    val claims: List<HealthcareClaim>
  )

  private data class FinBundleB(
    val households: List<Household>,
    val disputes: List<DisputeRecord>,
    val approvals: List<ApprovalRequest>,
    val audits: List<AuditLog>,
    val users: List<UserAccount>
  )
}
