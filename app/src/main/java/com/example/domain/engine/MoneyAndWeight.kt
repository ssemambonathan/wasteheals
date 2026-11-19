package com.example.domain.engine

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Exact decimal precision utilities for WasteHeals financial and mass calculations.
 * Never uses binary floating-point (Float/Double) for UGX or kilogram arithmetic.
 *
 * Storage representation:
 * - Currency (UGX minor units): Long whole UGX shillings (scale 0) or BigDecimal(scale = 2) in calculations.
 * - Mass (milli-kilograms / grams): Long milli-kg where 1,000 milli-kg == 1.000 kg (scale 3).
 */
object MoneyAndWeight {
  private val THOUSAND = BigDecimal("1000")
  private val HUNDRED = BigDecimal("100")

  fun milliKgToBigDecimal(milliKg: Long): BigDecimal =
    BigDecimal.valueOf(milliKg).divide(THOUSAND, 3, RoundingMode.HALF_UP)

  fun bigDecimalKgToMilli(kg: BigDecimal): Long =
    kg.setScale(3, RoundingMode.HALF_UP).multiply(THOUSAND).longValueExact()

  fun parseKgToMilli(input: String): Long? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    val bd = trimmed.toBigDecimalOrNull() ?: return null
    if (bd < BigDecimal.ZERO) return null
    return bigDecimalKgToMilli(bd)
  }

  fun parseUgxToLong(input: String): Long? {
    val trimmed = input.trim().replace(",", "")
    if (trimmed.isEmpty()) return null
    val bd = trimmed.toBigDecimalOrNull() ?: return null
    if (bd < BigDecimal.ZERO) return null
    return bd.setScale(0, RoundingMode.HALF_UP).longValueExact()
  }

  /**
   * Computes exact earnings in whole UGX:
   * (acceptedMilliKg / 1000) * unitPriceUgxPerKg, rounded HALF_UP to nearest UGX.
   */
  fun calculateAmountUgx(acceptedMilliKg: Long, unitPriceUgxPerKg: Long): Long {
    if (acceptedMilliKg <= 0L || unitPriceUgxPerKg <= 0L) return 0L
    val kg = milliKgToBigDecimal(acceptedMilliKg)
    val rate = BigDecimal.valueOf(unitPriceUgxPerKg)
    return kg.multiply(rate).setScale(0, RoundingMode.HALF_UP).longValueExact()
  }

  /**
   * Computes exact percentage share of a UGX base amount using BigDecimal HALF_UP.
   */
  fun calculatePercentageUgx(baseUgx: Long, pct: Int): Long {
    if (baseUgx <= 0L || pct <= 0) return 0L
    return BigDecimal.valueOf(baseUgx)
      .multiply(BigDecimal.valueOf(pct.toLong()))
      .divide(HUNDRED, 0, RoundingMode.HALF_UP)
      .longValueExact()
  }

  fun formatUgx(amountUgx: Long, currency: String = "UGX"): String {
    val nf = NumberFormat.getNumberInstance(Locale.US)
    return "$currency ${nf.format(amountUgx)}"
  }

  fun formatKg(milliKg: Long): String {
    val kg = milliKgToBigDecimal(milliKg)
    return "${kg.toPlainString()} kg"
  }
}
