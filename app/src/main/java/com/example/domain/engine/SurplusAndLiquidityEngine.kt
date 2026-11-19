package com.example.domain.engine

import java.math.BigDecimal
import java.math.RoundingMode

data class SurplusPolicyConfig(
  val version: Int = 1,
  val healthcareUgxPerKg: Long = 30L,
  val pickerRewardPct: Int = 30,
  val operationsPct: Int = 35,
  val contingencyPct: Int = 20,
  val expansionPct: Int = 15,
  val minWorkingCapitalUgx: Long = 2_000_000L
)

data class SurplusInput(
  val revenueUgx: Long,
  val cogsUgx: Long,
  val transportAndSortingUgx: Long,
  val healthcareContributionUgx: Long,
  val operatingExpensesUgx: Long,
  val rejectionAndLossReserveUgx: Long,
  val taxesAndObligationsUgx: Long,
  val operatingCashOnHandUgx: Long,
  val unsettledPickerPayablesUgx: Long,
  val dueHealthcareCashTransferUgx: Long,
  val dueOperatingPayablesUgx: Long,
  val contingencyLiquidityReserveUgx: Long
)

data class SurplusAllocationResult(
  val accountingSurplusUgx: Long,
  val availableDistributableCashUgx: Long,
  val liquidityGatePassed: Boolean,
  val blockedReason: String?,
  val distributablePoolUgx: Long,
  val pickerRewardPoolUgx: Long,
  val operationsAllocationUgx: Long,
  val contingencyAllocationUgx: Long,
  val expansionAllocationUgx: Long
)

data class PickerScoreInput(
  val pickerUid: String,
  val pickerName: String,
  val acceptedKgMilli: Long,
  val qualityAccuracyPct: Int, // 0..100
  val consistencyPct: Int,     // 0..100
  val safetyCompliancePct: Int // 0..100
)

data class PickerRewardAllocation(
  val pickerUid: String,
  val pickerName: String,
  val compositeScore: BigDecimal,
  val rewardUgx: Long
)

object SurplusAndLiquidityEngine {

  /**
   * Computes distributable surplus only after all business costs, healthcare contributions,
   * rejections/losses, and taxes, and enforces the Liquidity Protection Gate before releasing cash.
   */
  fun evaluateSurplusAndLiquidity(
    input: SurplusInput,
    policy: SurplusPolicyConfig
  ): SurplusAllocationResult {
    require(
      policy.pickerRewardPct + policy.operationsPct + policy.contingencyPct + policy.expansionPct == 100
    ) { "Policy allocation percentages must total 100%." }

    val totalCosts = input.cogsUgx +
      input.transportAndSortingUgx +
      input.healthcareContributionUgx +
      input.operatingExpensesUgx +
      input.rejectionAndLossReserveUgx +
      input.taxesAndObligationsUgx

    val accountingSurplus = input.revenueUgx - totalCosts

    val requiredCashCommitments = input.unsettledPickerPayablesUgx +
      input.dueHealthcareCashTransferUgx +
      input.dueOperatingPayablesUgx +
      input.taxesAndObligationsUgx +
      policy.minWorkingCapitalUgx +
      input.contingencyLiquidityReserveUgx

    val availableCash = input.operatingCashOnHandUgx - requiredCashCommitments

    if (accountingSurplus <= 0L) {
      return SurplusAllocationResult(
        accountingSurplusUgx = accountingSurplus,
        availableDistributableCashUgx = availableCash,
        liquidityGatePassed = false,
        blockedReason = "Distributable surplus is zero or negative (${MoneyAndWeight.formatUgx(accountingSurplus)}). Reward pool must remain UGX 0.",
        distributablePoolUgx = 0L,
        pickerRewardPoolUgx = 0L,
        operationsAllocationUgx = 0L,
        contingencyAllocationUgx = 0L,
        expansionAllocationUgx = 0L
      )
    }

    if (availableCash <= 0L) {
      return SurplusAllocationResult(
        accountingSurplusUgx = accountingSurplus,
        availableDistributableCashUgx = availableCash,
        liquidityGatePassed = false,
        blockedReason = "Liquidity protection gate failed: available cash after picker payables, healthcare due, working capital (${MoneyAndWeight.formatUgx(policy.minWorkingCapitalUgx)}), and reserves is ${MoneyAndWeight.formatUgx(availableCash)}.",
        distributablePoolUgx = 0L,
        pickerRewardPoolUgx = 0L,
        operationsAllocationUgx = 0L,
        contingencyAllocationUgx = 0L,
        expansionAllocationUgx = 0L
      )
    }

    // Cap distribution to the lesser of accounting surplus and available free cash
    val distributablePool = minOf(accountingSurplus, availableCash)
    val rewardPool = MoneyAndWeight.calculatePercentageUgx(distributablePool, policy.pickerRewardPct)
    val opsPool = MoneyAndWeight.calculatePercentageUgx(distributablePool, policy.operationsPct)
    val contingencyPool = MoneyAndWeight.calculatePercentageUgx(distributablePool, policy.contingencyPct)
    val expansionPool = distributablePool - rewardPool - opsPool - contingencyPool

    return SurplusAllocationResult(
      accountingSurplusUgx = accountingSurplus,
      availableDistributableCashUgx = availableCash,
      liquidityGatePassed = true,
      blockedReason = if (availableCash < accountingSurplus) {
        "Distribution capped at available cash (${MoneyAndWeight.formatUgx(availableCash)}) rather than full accounting surplus."
      } else null,
      distributablePoolUgx = distributablePool,
      pickerRewardPoolUgx = rewardPool,
      operationsAllocationUgx = opsPool,
      contingencyAllocationUgx = contingencyPool,
      expansionAllocationUgx = expansionPool
    )
  }

  /**
   * Allocates the picker performance reward pool proportionally using:
   * 50% verified accepted kg (normalized against max), 25% quality, 15% consistency, 10% safety/PPE.
   */
  fun distributePickerRewards(
    rewardPoolUgx: Long,
    pickers: List<PickerScoreInput>
  ): List<PickerRewardAllocation> {
    if (rewardPoolUgx <= 0L || pickers.isEmpty()) {
      return pickers.map {
        PickerRewardAllocation(it.pickerUid, it.pickerName, BigDecimal.ZERO, 0L)
      }
    }
    val maxKg = pickers.maxOf { it.acceptedKgMilli }.coerceAtLeast(1L)
    val wVol = BigDecimal("0.50")
    val wQual = BigDecimal("0.25")
    val wCons = BigDecimal("0.15")
    val wSafe = BigDecimal("0.10")
    val hundred = BigDecimal("100")

    val scores = pickers.map { p ->
      val volNorm = BigDecimal.valueOf(p.acceptedKgMilli)
        .divide(BigDecimal.valueOf(maxKg), 6, RoundingMode.HALF_UP)
      val qualNorm = BigDecimal.valueOf(p.qualityAccuracyPct.coerceIn(0, 100).toLong())
        .divide(hundred, 6, RoundingMode.HALF_UP)
      val consNorm = BigDecimal.valueOf(p.consistencyPct.coerceIn(0, 100).toLong())
        .divide(hundred, 6, RoundingMode.HALF_UP)
      val safeNorm = BigDecimal.valueOf(p.safetyCompliancePct.coerceIn(0, 100).toLong())
        .divide(hundred, 6, RoundingMode.HALF_UP)
      val composite = volNorm.multiply(wVol)
        .add(qualNorm.multiply(wQual))
        .add(consNorm.multiply(wCons))
        .add(safeNorm.multiply(wSafe))
        .setScale(4, RoundingMode.HALF_UP)
      p to composite
    }

    val totalScore = scores.fold(BigDecimal.ZERO) { acc, pair -> acc.add(pair.second) }
    if (totalScore <= BigDecimal.ZERO) {
      return scores.map { (p, score) ->
        PickerRewardAllocation(p.pickerUid, p.pickerName, score, 0L)
      }
    }

    var remaining = rewardPoolUgx
    return scores.mapIndexed { index, (p, score) ->
      val reward = if (index == scores.lastIndex) {
        remaining.coerceAtLeast(0L)
      } else {
        val share = BigDecimal.valueOf(rewardPoolUgx)
          .multiply(score)
          .divide(totalScore, 0, RoundingMode.HALF_UP)
          .longValueExact()
          .coerceAtMost(remaining)
        remaining -= share
        share
      }
      PickerRewardAllocation(p.pickerUid, p.pickerName, score, reward)
    }
  }
}
