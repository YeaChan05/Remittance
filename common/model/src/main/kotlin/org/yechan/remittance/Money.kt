package org.yechan.remittance

import java.math.BigDecimal
import java.math.RoundingMode

class Money private constructor(
    private val value: BigDecimal,
) : Comparable<Money> {
    val amount: BigDecimal
        get() = value

    fun add(other: Money): Money = Money(normalize(value.add(other.value)))

    fun subtract(other: Money): Money = Money(normalize(value.subtract(other.value)))

    fun multiply(
        multiplier: BigDecimal,
        roundingMode: RoundingMode = DEFAULT_ROUNDING_MODE,
    ): Money = Money(value.multiply(multiplier).setScale(SCALE, roundingMode))

    fun isPositive(): Boolean = value > BigDecimal.ZERO

    fun isZero(): Boolean = value.compareTo(BigDecimal.ZERO) == 0

    override fun compareTo(other: Money): Int = value.compareTo(other.value)

    override fun equals(other: Any?): Boolean = other is Money && compareTo(other) == 0

    override fun hashCode(): Int = value.stripTrailingZeros().hashCode()

    override fun toString(): String = value.toPlainString()

    companion object {
        private const val SCALE = 2
        private val DEFAULT_ROUNDING_MODE = RoundingMode.DOWN
        private val ZERO = Money(BigDecimal.ZERO.setScale(SCALE, DEFAULT_ROUNDING_MODE))

        @JvmStatic
        fun of(value: BigDecimal): Money = Money(normalize(value))

        @JvmStatic
        fun of(value: Long): Money = of(BigDecimal.valueOf(value))

        @JvmStatic
        fun zero(): Money = ZERO

        private fun normalize(value: BigDecimal): BigDecimal = value.setScale(SCALE, DEFAULT_ROUNDING_MODE)
    }
}
