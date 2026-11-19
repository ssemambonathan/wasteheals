package com.example

import com.example.domain.engine.ChartOfAccounts
import com.example.domain.engine.DoubleEntryAccountingEngine
import com.example.domain.engine.MoMoCallbackPayload
import com.example.domain.engine.MoMoDisbursementRequest
import com.example.domain.engine.MoMoPaymentEngine
import com.example.domain.engine.MoneyAndWeight
import com.example.domain.engine.PickerScoreInput
import com.example.domain.engine.PricingAndWeighingEngine
import com.example.domain.engine.SandboxScenario
import com.example.domain.engine.SurplusAndLiquidityEngine
import com.example.domain.engine.SurplusInput
import com.example.domain.engine.SurplusPolicyConfig
import com.example.domain.model.MoMoLifecycleStatus
import com.example.domain.model.MoMoProvider
import com.example.domain.model.ReconciliationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WasteHealsFinancialEnginesTest {

  @Test
  fun `exact BigDecimal money and weight calculation avoids binary float drift`() {
    val gross = MoneyAndWeight.parseKgToMilli("25.755")!!
    val tare = MoneyAndWeight.parseKgToMilli("1.255")!!
    val deduct = MoneyAndWeight.parseKgToMilli("0.500")!!
    val result = PricingAndWeighingEngine.calculate(
      grossKgMilli = gross,
      tareKgMilli = tare,
      deductionKgMilli = deduct,
      unitPriceUgxMinor = 900L
    ).getOrThrow()

    assertEquals(24_500L, result.netKgMilli)
    assertEquals(24_000L, result.acceptedKgMilli)
    assertEquals(21_600L, result.totalEarningsUgxMinor)
  }

  @Test
  fun `tare greater or equal to gross is rejected`() {
    val failure = PricingAndWeighingEngine.calculate(
      grossKgMilli = 5_000L,
      tareKgMilli = 5_000L,
      deductionKgMilli = 0L,
      unitPriceUgxMinor = 900L
    )
    assertTrue(failure.isFailure)
  }

  @Test
  fun `double entry delivery, confirmed sale, and buyer cash receipt stay balanced and fund UGX 30 per kg`() {
    val je1 = DoubleEntryAccountingEngine.forDeliveryAcceptance(
      entryRef = "JE-1",
      periodId = "2026-11",
      deliveryId = "del_1",
      hubId = "hub_nakawa",
      earningsUgx = 90_000L,
      materialName = "Clear PET Bottles",
      acceptedKgMilli = 100_000L
    ).getOrThrow()

    // Sell 100.000 kg @ UGX 1,450/kg = UGX 145,000; HC accrual = 100 * 30 = UGX 3,000
    val je2 = DoubleEntryAccountingEngine.forConfirmedSale(
      entryRef = "JE-2",
      periodId = "2026-11",
      saleId = "sale_1",
      hubId = "hub_nakawa",
      invoiceTotalUgx = 145_000L,
      inventoryCostUgx = 90_000L,
      confirmedSoldKgMilli = 100_000L,
      healthcareUgxPerKg = 30L,
      buyerName = "Kampala EcoPolymers"
    ).getOrThrow()

    // Before buyer cash receipt: Restricted HC Cash = 0, Unfunded Accrued = 3,000
    val preReceipt = DoubleEntryAccountingEngine.summarizeLedger(je1.lines + je2.lines)
    assertTrue(preReceipt.isBalanced)
    assertEquals(0L, preReceipt.restrictedHealthcareCashUgx)
    assertEquals(3_000L, preReceipt.healthcareAccruedUnfundedUgx)

    // Buyer pays full UGX 145,000 -> UGX 3,000 moves to Restricted HC Cash
    val je3 = DoubleEntryAccountingEngine.forBuyerCashReceipt(
      entryRef = "JE-3",
      periodId = "2026-11",
      saleId = "sale_1",
      receiptSeq = "01",
      hubId = "hub_nakawa",
      cashReceivedUgx = 145_000L,
      healthcareFundedDeltaUgx = 3_000L,
      buyerName = "Kampala EcoPolymers"
    ).getOrThrow()

    val postReceipt = DoubleEntryAccountingEngine.summarizeLedger(je1.lines + je2.lines + je3.lines)
    assertTrue(postReceipt.isBalanced)
    assertEquals(3_000L, postReceipt.restrictedHealthcareCashUgx)
    assertEquals(0L, postReceipt.healthcareAccruedUnfundedUgx)
    assertEquals(3_000L, postReceipt.healthcareFundedAvailableUgx)
    assertEquals(142_000L, postReceipt.operatingCashUgx)
    assertEquals(90_000L, postReceipt.pickerEarningsPayableUgx)
  }

  @Test
  fun `zero or negative surplus and failed liquidity gate block reward distribution`() {
    val policy = SurplusPolicyConfig(minWorkingCapitalUgx = 2_000_000L)

    // Case 1: Negative accounting surplus
    val neg = SurplusAndLiquidityEngine.evaluateSurplusAndLiquidity(
      SurplusInput(
        revenueUgx = 1_000_000L,
        cogsUgx = 900_000L,
        transportAndSortingUgx = 200_000L,
        healthcareContributionUgx = 30_000L,
        operatingExpensesUgx = 50_000L,
        rejectionAndLossReserveUgx = 0L,
        taxesAndObligationsUgx = 0L,
        operatingCashOnHandUgx = 10_000_000L,
        unsettledPickerPayablesUgx = 500_000L,
        dueHealthcareCashTransferUgx = 0L,
        dueOperatingPayablesUgx = 0L,
        contingencyLiquidityReserveUgx = 500_000L
      ),
      policy
    )
    assertFalse(neg.liquidityGatePassed)
    assertEquals(0L, neg.pickerRewardPoolUgx)

    // Case 2: Positive surplus but insufficient free cash after commitments + min working capital
    val illiquid = SurplusAndLiquidityEngine.evaluateSurplusAndLiquidity(
      SurplusInput(
        revenueUgx = 5_000_000L,
        cogsUgx = 2_500_000L,
        transportAndSortingUgx = 300_000L,
        healthcareContributionUgx = 150_000L,
        operatingExpensesUgx = 200_000L,
        rejectionAndLossReserveUgx = 0L,
        taxesAndObligationsUgx = 0L,
        operatingCashOnHandUgx = 2_500_000L,
        unsettledPickerPayablesUgx = 700_000L,
        dueHealthcareCashTransferUgx = 150_000L,
        dueOperatingPayablesUgx = 0L,
        contingencyLiquidityReserveUgx = 300_000L
      ),
      policy
    )
    assertFalse(illiquid.liquidityGatePassed)
    assertEquals(0L, illiquid.pickerRewardPoolUgx)

    // Case 3: Healthy surplus and liquidity -> exact 30% reward pool & weighted sum matches pool
    val healthy = SurplusAndLiquidityEngine.evaluateSurplusAndLiquidity(
      SurplusInput(
        revenueUgx = 10_000_000L,
        cogsUgx = 5_000_000L,
        transportAndSortingUgx = 500_000L,
        healthcareContributionUgx = 300_000L,
        operatingExpensesUgx = 200_000L,
        rejectionAndLossReserveUgx = 0L,
        taxesAndObligationsUgx = 0L,
        operatingCashOnHandUgx = 12_000_000L,
        unsettledPickerPayablesUgx = 1_000_000L,
        dueHealthcareCashTransferUgx = 0L,
        dueOperatingPayablesUgx = 0L,
        contingencyLiquidityReserveUgx = 500_000L
      ),
      policy
    )
    assertTrue(healthy.liquidityGatePassed)
    assertEquals(1_200_000L, healthy.pickerRewardPoolUgx) // 30% of 4,000,000

    val allocations = SurplusAndLiquidityEngine.distributePickerRewards(
      healthy.pickerRewardPoolUgx,
      listOf(
        PickerScoreInput("p1", "Amina", 500_000L, 98, 95, 100),
        PickerScoreInput("p2", "Okello", 250_000L, 90, 80, 95)
      )
    )
    assertEquals(1_200_000L, allocations.sumOf { it.rewardUgx })
    assertTrue(allocations[0].rewardUgx > allocations[1].rewardUgx)
  }

  @Test
  fun `momo timeout locks UNKNOWN and callback verification checks HMAC and transitions`() {
    val req = MoMoDisbursementRequest(
      internalPayoutId = "pay_001",
      idempotencyKey = "idem_momo_001",
      provider = MoMoProvider.MTN_MOMO,
      destinationMsisdnMasked = "+25677***123",
      amountUgxMinor = 25_000L
    )
    val timeout = MoMoPaymentEngine.simulateSandboxDisbursement(req, SandboxScenario.NETWORK_TIMEOUT)
    assertEquals(MoMoLifecycleStatus.UNKNOWN, timeout.nextLifecycleStatus)
    assertEquals(ReconciliationStatus.UNRESOLVED, timeout.reconciliationStatus)
    assertFalse(timeout.canRetryAutomatically)

    val secret = "sandbox_webhook_hmac_test_secret"
    val canonical = "MTN-001|idem_momo_001|25000|UGX|SUCCESSFUL"
    val validSig = MoMoPaymentEngine.computeWebhookHmacHex(canonical, secret)

    val verified = MoMoPaymentEngine.verifyCallback(
      expected = req,
      currentStatus = MoMoLifecycleStatus.UNKNOWN,
      callback = MoMoCallbackPayload(
        providerRef = "MTN-001",
        idempotencyKey = "idem_momo_001",
        amountUgxMinor = 25_000L,
        currency = "UGX",
        destinationMasked = "+25677***123",
        providerStatus = "SUCCESSFUL",
        signatureHex = validSig
      ),
      webhookSecret = secret,
      alreadyProcessedRef = false
    )
    assertTrue(verified.isSuccess)
    assertEquals(MoMoLifecycleStatus.SUCCESS, verified.getOrNull())

    // Out-of-order / duplicate callback after SUCCESS must be rejected
    val dup = MoMoPaymentEngine.verifyCallback(
      expected = req,
      currentStatus = MoMoLifecycleStatus.SUCCESS,
      callback = MoMoCallbackPayload(
        providerRef = "MTN-001",
        idempotencyKey = "idem_momo_001",
        amountUgxMinor = 25_000L,
        currency = "UGX",
        destinationMasked = "+25677***123",
        providerStatus = "PENDING",
        signatureHex = MoMoPaymentEngine.computeWebhookHmacHex("MTN-001|idem_momo_001|25000|UGX|PENDING", secret)
      ),
      webhookSecret = secret,
      alreadyProcessedRef = true
    )
    assertTrue(dup.isFailure)
  }
}
