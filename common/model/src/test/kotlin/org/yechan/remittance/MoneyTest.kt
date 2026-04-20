package org.yechan.remittance

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode

class MoneyTest {
    @Test
    fun `normalizes to two decimal places`() {
        val money = Money.of(BigDecimal("100.129"))

        assertThat(money.amount).isEqualByComparingTo("100.12")
    }

    @Test
    fun `adds and subtracts money safely`() {
        val base = Money.of(BigDecimal("100.10"))
        val delta = Money.of(BigDecimal("25.55"))

        assertThat(base.add(delta).amount).isEqualByComparingTo("125.65")
        assertThat(base.subtract(delta).amount).isEqualByComparingTo("74.55")
    }

    @Test
    fun `multiplies with explicit rounding`() {
        val fee = Money.of(BigDecimal("100.129")).multiply(BigDecimal("0.01"), RoundingMode.DOWN)

        assertThat(fee.amount).isEqualByComparingTo("1.00")
    }
}
