package com.example.domain.engine

data class WeighingCalculation(
  val grossKgMilli: Long,
  val tareKgMilli: Long,
  val netKgMilli: Long,
  val deductionKgMilli: Long,
  val acceptedKgMilli: Long,
  val unitPriceUgxMinor: Long,
  val totalEarningsUgxMinor: Long
)

object PricingAndWeighingEngine {
  /**
   * Validates calibrated scale inputs and computes net/accepted mass and guaranteed earnings
   * using an immutable snapshot of the approved purchase rate.
   */
  fun calculate(
    grossKgMilli: Long,
    tareKgMilli: Long,
    deductionKgMilli: Long,
    unitPriceUgxMinor: Long
  ): Result<WeighingCalculation> {
    if (grossKgMilli <= 0L) {
      return Result.failure(IllegalArgumentException("Gross weight must be greater than 0 kg."))
    }
    if (tareKgMilli < 0L || tareKgMilli >= grossKgMilli) {
      return Result.failure(IllegalArgumentException("Tare weight must be >= 0 and less than gross weight."))
    }
    val netKgMilli = grossKgMilli - tareKgMilli
    if (deductionKgMilli < 0L || deductionKgMilli > netKgMilli) {
      return Result.failure(IllegalArgumentException("Quality deduction cannot exceed net weight."))
    }
    if (unitPriceUgxMinor < 0L) {
      return Result.failure(IllegalArgumentException("Unit rate cannot be negative."))
    }
    val acceptedKgMilli = netKgMilli - deductionKgMilli
    val earningsUgx = MoneyAndWeight.calculateAmountUgx(acceptedKgMilli, unitPriceUgxMinor)
    return Result.success(
      WeighingCalculation(
        grossKgMilli = grossKgMilli,
        tareKgMilli = tareKgMilli,
        netKgMilli = netKgMilli,
        deductionKgMilli = deductionKgMilli,
        acceptedKgMilli = acceptedKgMilli,
        unitPriceUgxMinor = unitPriceUgxMinor,
        totalEarningsUgxMinor = earningsUgx
      )
    )
  }
}
