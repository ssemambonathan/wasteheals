package com.example.domain.engine

import com.example.domain.model.AccountCategory

data class DraftJournalLine(
  val accountCode: String,
  val accountName: String,
  val accountCategory: AccountCategory,
  val debitUgxMinor: Long,
  val creditUgxMinor: Long,
  val hubId: String
)

data class BalancedJournalBundle(
  val entryRef: String,
  val periodId: String,
  val eventType: String,
  val sourceId: String,
  val idempotencyKey: String,
  val description: String,
  val totalDebitsUgxMinor: Long,
  val totalCreditsUgxMinor: Long,
  val currency: String,
  val lines: List<DraftJournalLine>
)

object ChartOfAccounts {
  const val CASH_OPERATING_CODE = "1010"
  const val CASH_OPERATING_NAME = "Operating Cash (Bank / MoMo Float)"

  const val CASH_RESTRICTED_HC_CODE = "1020"
  const val CASH_RESTRICTED_HC_NAME = "Restricted Healthcare Cash Fund"

  const val BUYER_RECEIVABLE_CODE = "1100"
  const val BUYER_RECEIVABLE_NAME = "Buyer Accounts Receivable"

  const val INVENTORY_WASTE_CODE = "1200"
  const val INVENTORY_WASTE_NAME = "Recovered Material Inventory"

  const val PICKER_PAYABLE_CODE = "2010"
  const val PICKER_PAYABLE_NAME = "Guaranteed Picker Earnings Payable"

  const val HC_OBLIGATION_ACCRUED_CODE = "2110"
  const val HC_OBLIGATION_ACCRUED_NAME = "Healthcare Contribution Accrued (Unfunded)"

  const val HC_OBLIGATION_FUNDED_CODE = "2120"
  const val HC_OBLIGATION_FUNDED_NAME = "Healthcare Fund Liability (Funded)"

  const val SALES_REVENUE_CODE = "4010"
  const val SALES_REVENUE_NAME = "Recovered Material & Compost Sales Revenue"

  const val COGS_MATERIAL_CODE = "5010"
  const val COGS_MATERIAL_NAME = "Cost of Sold Waste Inventory"

  const val HC_CONTRIBUTION_EXPENSE_CODE = "5110"
  const val HC_CONTRIBUTION_EXPENSE_NAME = "Healthcare Contribution Expense (UGX 30/kg)"

  const val PROCESSING_EXPENSE_CODE = "5200"
  const val PROCESSING_EXPENSE_NAME = "Sorting & Organic Processing Expense"
}

data class TrialBalanceRow(
  val accountCode: String,
  val accountName: String,
  val category: AccountCategory,
  val totalDebitsUgx: Long,
  val totalCreditsUgx: Long
) {
  val netBalanceUgx: Long
    get() = when (category) {
      AccountCategory.ASSET, AccountCategory.EXPENSE, AccountCategory.RESTRICTED_FUND ->
        totalDebitsUgx - totalCreditsUgx
      AccountCategory.LIABILITY, AccountCategory.EQUITY, AccountCategory.REVENUE ->
        totalCreditsUgx - totalDebitsUgx
    }
}

data class FinancialStatementsSummary(
  val totalDebitsUgx: Long,
  val totalCreditsUgx: Long,
  val isBalanced: Boolean,
  val operatingCashUgx: Long,
  val restrictedHealthcareCashUgx: Long,
  val buyerReceivablesUgx: Long,
  val inventoryValuationUgx: Long,
  val pickerEarningsPayableUgx: Long,
  val healthcareAccruedUnfundedUgx: Long,
  val healthcareFundedAvailableUgx: Long,
  val salesRevenueUgx: Long,
  val totalExpensesAndCogsUgx: Long,
  val accountingNetSurplusUgx: Long,
  val trialBalance: List<TrialBalanceRow>
)

object DoubleEntryAccountingEngine {

  fun buildAndVerify(
    entryRef: String,
    periodId: String,
    eventType: String,
    sourceId: String,
    idempotencyKey: String,
    description: String,
    lines: List<DraftJournalLine>,
    currency: String = "UGX"
  ): Result<BalancedJournalBundle> {
    if (lines.isEmpty()) {
      return Result.failure(IllegalArgumentException("Journal entry must have at least two lines."))
    }
    for (line in lines) {
      if (line.debitUgxMinor < 0L || line.creditUgxMinor < 0L) {
        return Result.failure(IllegalArgumentException("Journal line amounts cannot be negative."))
      }
      if ((line.debitUgxMinor > 0L) == (line.creditUgxMinor > 0L)) {
        return Result.failure(IllegalArgumentException("Each line must have either a positive debit or credit, not both or zero."))
      }
    }
    val totalDebits = lines.sumOf { it.debitUgxMinor }
    val totalCredits = lines.sumOf { it.creditUgxMinor }
    if (totalDebits <= 0L || totalDebits != totalCredits) {
      return Result.failure(
        IllegalStateException("Unbalanced journal entry: debits ($totalDebits) != credits ($totalCredits).")
      )
    }
    return Result.success(
      BalancedJournalBundle(
        entryRef = entryRef,
        periodId = periodId,
        eventType = eventType,
        sourceId = sourceId,
        idempotencyKey = idempotencyKey,
        description = description,
        totalDebitsUgxMinor = totalDebits,
        totalCreditsUgxMinor = totalCredits,
        currency = currency,
        lines = lines
      )
    )
  }

  /**
   * Delivery Acceptance: DR Inventory / CR Guaranteed Picker Earnings Payable.
   */
  fun forDeliveryAcceptance(
    entryRef: String,
    periodId: String,
    deliveryId: String,
    hubId: String,
    earningsUgx: Long,
    materialName: String,
    acceptedKgMilli: Long
  ): Result<BalancedJournalBundle> = buildAndVerify(
    entryRef = entryRef,
    periodId = periodId,
    eventType = "DELIVERY_ACCEPTANCE",
    sourceId = deliveryId,
    idempotencyKey = "je_del_$deliveryId",
    description = "Accepted ${MoneyAndWeight.formatKg(acceptedKgMilli)} $materialName",
    lines = listOf(
      DraftJournalLine(
        ChartOfAccounts.INVENTORY_WASTE_CODE,
        ChartOfAccounts.INVENTORY_WASTE_NAME,
        AccountCategory.ASSET,
        debitUgxMinor = earningsUgx,
        creditUgxMinor = 0L,
        hubId = hubId
      ),
      DraftJournalLine(
        ChartOfAccounts.PICKER_PAYABLE_CODE,
        ChartOfAccounts.PICKER_PAYABLE_NAME,
        AccountCategory.LIABILITY,
        debitUgxMinor = 0L,
        creditUgxMinor = earningsUgx,
        hubId = hubId
      )
    )
  )

  /**
   * Direct MoMo Payout Settlement: DR Picker Payable / CR Operating Cash.
   */
  fun forPickerPayoutSuccess(
    entryRef: String,
    periodId: String,
    payoutId: String,
    hubId: String,
    amountUgx: Long,
    pickerName: String
  ): Result<BalancedJournalBundle> = buildAndVerify(
    entryRef = entryRef,
    periodId = periodId,
    eventType = "PICKER_MOMO_PAYOUT",
    sourceId = payoutId,
    idempotencyKey = "je_pay_$payoutId",
    description = "MoMo payout settled for $pickerName",
    lines = listOf(
      DraftJournalLine(
        ChartOfAccounts.PICKER_PAYABLE_CODE,
        ChartOfAccounts.PICKER_PAYABLE_NAME,
        AccountCategory.LIABILITY,
        debitUgxMinor = amountUgx,
        creditUgxMinor = 0L,
        hubId = hubId
      ),
      DraftJournalLine(
        ChartOfAccounts.CASH_OPERATING_CODE,
        ChartOfAccounts.CASH_OPERATING_NAME,
        AccountCategory.ASSET,
        debitUgxMinor = 0L,
        creditUgxMinor = amountUgx,
        hubId = hubId
      )
    )
  )

  /**
   * Confirmed Buyer Sale:
   * 1. DR Buyer Receivable / CR Sales Revenue
   * 2. DR COGS / CR Inventory (if cost > 0)
   * 3. DR Healthcare Expense / CR Healthcare Accrued Unfunded (at UGX 30/kg * confirmed sold kg)
   */
  fun forConfirmedSale(
    entryRef: String,
    periodId: String,
    saleId: String,
    hubId: String,
    invoiceTotalUgx: Long,
    inventoryCostUgx: Long,
    confirmedSoldKgMilli: Long,
    healthcareUgxPerKg: Long,
    buyerName: String
  ): Result<BalancedJournalBundle> {
    val lines = mutableListOf<DraftJournalLine>()
    if (invoiceTotalUgx > 0L) {
      lines += DraftJournalLine(
        ChartOfAccounts.BUYER_RECEIVABLE_CODE,
        ChartOfAccounts.BUYER_RECEIVABLE_NAME,
        AccountCategory.ASSET,
        debitUgxMinor = invoiceTotalUgx,
        creditUgxMinor = 0L,
        hubId = hubId
      )
      lines += DraftJournalLine(
        ChartOfAccounts.SALES_REVENUE_CODE,
        ChartOfAccounts.SALES_REVENUE_NAME,
        AccountCategory.REVENUE,
        debitUgxMinor = 0L,
        creditUgxMinor = invoiceTotalUgx,
        hubId = hubId
      )
    }
    if (inventoryCostUgx > 0L) {
      lines += DraftJournalLine(
        ChartOfAccounts.COGS_MATERIAL_CODE,
        ChartOfAccounts.COGS_MATERIAL_NAME,
        AccountCategory.EXPENSE,
        debitUgxMinor = inventoryCostUgx,
        creditUgxMinor = 0L,
        hubId = hubId
      )
      lines += DraftJournalLine(
        ChartOfAccounts.INVENTORY_WASTE_CODE,
        ChartOfAccounts.INVENTORY_WASTE_NAME,
        AccountCategory.ASSET,
        debitUgxMinor = 0L,
        creditUgxMinor = inventoryCostUgx,
        hubId = hubId
      )
    }
    val healthcareAccrualUgx = MoneyAndWeight.calculateAmountUgx(confirmedSoldKgMilli, healthcareUgxPerKg)
    if (healthcareAccrualUgx > 0L) {
      lines += DraftJournalLine(
        ChartOfAccounts.HC_CONTRIBUTION_EXPENSE_CODE,
        ChartOfAccounts.HC_CONTRIBUTION_EXPENSE_NAME,
        AccountCategory.EXPENSE,
        debitUgxMinor = healthcareAccrualUgx,
        creditUgxMinor = 0L,
        hubId = hubId
      )
      lines += DraftJournalLine(
        ChartOfAccounts.HC_OBLIGATION_ACCRUED_CODE,
        ChartOfAccounts.HC_OBLIGATION_ACCRUED_NAME,
        AccountCategory.LIABILITY,
        debitUgxMinor = 0L,
        creditUgxMinor = healthcareAccrualUgx,
        hubId = hubId
      )
    }
    return buildAndVerify(
      entryRef = entryRef,
      periodId = periodId,
      eventType = "CONFIRMED_BUYER_SALE",
      sourceId = saleId,
      idempotencyKey = "je_sale_$saleId",
      description = "Confirmed ${MoneyAndWeight.formatKg(confirmedSoldKgMilli)} sale to $buyerName",
      lines = lines
    )
  }

  /**
   * Buyer Cash Receipt:
   * 1. DR Operating Cash / CR Buyer Receivable
   * 2. If healthcareFundedDeltaUgx > 0:
   *    DR Restricted Healthcare Cash / CR Operating Cash
   *    DR Healthcare Accrued Unfunded / CR Healthcare Fund Liability (Funded)
   */
  fun forBuyerCashReceipt(
    entryRef: String,
    periodId: String,
    saleId: String,
    receiptSeq: String,
    hubId: String,
    cashReceivedUgx: Long,
    healthcareFundedDeltaUgx: Long,
    buyerName: String
  ): Result<BalancedJournalBundle> {
    val lines = mutableListOf(
      DraftJournalLine(
        ChartOfAccounts.CASH_OPERATING_CODE,
        ChartOfAccounts.CASH_OPERATING_NAME,
        AccountCategory.ASSET,
        debitUgxMinor = cashReceivedUgx,
        creditUgxMinor = 0L,
        hubId = hubId
      ),
      DraftJournalLine(
        ChartOfAccounts.BUYER_RECEIVABLE_CODE,
        ChartOfAccounts.BUYER_RECEIVABLE_NAME,
        AccountCategory.ASSET,
        debitUgxMinor = 0L,
        creditUgxMinor = cashReceivedUgx,
        hubId = hubId
      )
    )
    if (healthcareFundedDeltaUgx > 0L) {
      lines += DraftJournalLine(
        ChartOfAccounts.CASH_RESTRICTED_HC_CODE,
        ChartOfAccounts.CASH_RESTRICTED_HC_NAME,
        AccountCategory.RESTRICTED_FUND,
        debitUgxMinor = healthcareFundedDeltaUgx,
        creditUgxMinor = 0L,
        hubId = hubId
      )
      lines += DraftJournalLine(
        ChartOfAccounts.CASH_OPERATING_CODE,
        ChartOfAccounts.CASH_OPERATING_NAME,
        AccountCategory.ASSET,
        debitUgxMinor = 0L,
        creditUgxMinor = healthcareFundedDeltaUgx,
        hubId = hubId
      )
      lines += DraftJournalLine(
        ChartOfAccounts.HC_OBLIGATION_ACCRUED_CODE,
        ChartOfAccounts.HC_OBLIGATION_ACCRUED_NAME,
        AccountCategory.LIABILITY,
        debitUgxMinor = healthcareFundedDeltaUgx,
        creditUgxMinor = 0L,
        hubId = hubId
      )
      lines += DraftJournalLine(
        ChartOfAccounts.HC_OBLIGATION_FUNDED_CODE,
        ChartOfAccounts.HC_OBLIGATION_FUNDED_NAME,
        AccountCategory.LIABILITY,
        debitUgxMinor = 0L,
        creditUgxMinor = healthcareFundedDeltaUgx,
        hubId = hubId
      )
    }
    return buildAndVerify(
      entryRef = entryRef,
      periodId = periodId,
      eventType = "BUYER_CASH_RECEIPT",
      sourceId = saleId,
      idempotencyKey = "je_rcpt_${saleId}_$receiptSeq",
      description = "Receipt from $buyerName (${MoneyAndWeight.formatUgx(cashReceivedUgx)})",
      lines = lines
    )
  }

  /**
   * Healthcare Clinic Claim Settlement:
   * DR Healthcare Fund Liability (Funded) / CR Restricted Healthcare Cash Fund.
   */
  fun forHealthcareClaimPaid(
    entryRef: String,
    periodId: String,
    claimId: String,
    hubId: String,
    amountUgx: Long,
    clinicName: String
  ): Result<BalancedJournalBundle> = buildAndVerify(
    entryRef = entryRef,
    periodId = periodId,
    eventType = "HEALTHCARE_CLAIM_PAID",
    sourceId = claimId,
    idempotencyKey = "je_hc_$claimId",
    description = "Clinic claim paid to $clinicName",
    lines = listOf(
      DraftJournalLine(
        ChartOfAccounts.HC_OBLIGATION_FUNDED_CODE,
        ChartOfAccounts.HC_OBLIGATION_FUNDED_NAME,
        AccountCategory.LIABILITY,
        debitUgxMinor = amountUgx,
        creditUgxMinor = 0L,
        hubId = hubId
      ),
      DraftJournalLine(
        ChartOfAccounts.CASH_RESTRICTED_HC_CODE,
        ChartOfAccounts.CASH_RESTRICTED_HC_NAME,
        AccountCategory.RESTRICTED_FUND,
        debitUgxMinor = 0L,
        creditUgxMinor = amountUgx,
        hubId = hubId
      )
    )
  )

  fun summarizeLedger(lines: List<DraftJournalLine>): FinancialStatementsSummary {
    val grouped = lines.groupBy { it.accountCode }.map { (code, group) ->
      val first = group.first()
      TrialBalanceRow(
        accountCode = code,
        accountName = first.accountName,
        category = first.accountCategory,
        totalDebitsUgx = group.sumOf { it.debitUgxMinor },
        totalCreditsUgx = group.sumOf { it.creditUgxMinor }
      )
    }.sortedBy { it.accountCode }

    val totalDebits = grouped.sumOf { it.totalDebitsUgx }
    val totalCredits = grouped.sumOf { it.totalCreditsUgx }
    fun balanceOf(code: String): Long = grouped.firstOrNull { it.accountCode == code }?.netBalanceUgx ?: 0L

    val revenue = balanceOf(ChartOfAccounts.SALES_REVENUE_CODE)
    val expenses = grouped
      .filter { it.category == AccountCategory.EXPENSE }
      .sumOf { it.netBalanceUgx }

    return FinancialStatementsSummary(
      totalDebitsUgx = totalDebits,
      totalCreditsUgx = totalCredits,
      isBalanced = totalDebits == totalCredits,
      operatingCashUgx = balanceOf(ChartOfAccounts.CASH_OPERATING_CODE),
      restrictedHealthcareCashUgx = balanceOf(ChartOfAccounts.CASH_RESTRICTED_HC_CODE),
      buyerReceivablesUgx = balanceOf(ChartOfAccounts.BUYER_RECEIVABLE_CODE),
      inventoryValuationUgx = balanceOf(ChartOfAccounts.INVENTORY_WASTE_CODE),
      pickerEarningsPayableUgx = balanceOf(ChartOfAccounts.PICKER_PAYABLE_CODE),
      healthcareAccruedUnfundedUgx = balanceOf(ChartOfAccounts.HC_OBLIGATION_ACCRUED_CODE),
      healthcareFundedAvailableUgx = balanceOf(ChartOfAccounts.HC_OBLIGATION_FUNDED_CODE),
      salesRevenueUgx = revenue,
      totalExpensesAndCogsUgx = expenses,
      accountingNetSurplusUgx = revenue - expenses,
      trialBalance = grouped
    )
  }
}
