package org.yechan.remittance.transfer.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.yechan.remittance.transfer.TransferFailedException
import org.yechan.remittance.transfer.TransferProps
import java.math.BigDecimal

class DepositRequestTest {
    @Test
    fun `유효한 입금 요청을 생성한다`() {
        val request = DepositRequest(10L, BigDecimal("50.00"))

        assertEquals(TransferProps.TransferScopeValue.DEPOSIT, request.scope)
        assertEquals(BigDecimal.ZERO, request.fee)
        assertEquals(10L, request.fromAccountId)
        assertEquals(10L, request.toAccountId)
    }

    @Test
    fun `금액이 없으면 예외가 발생한다`() {
        assertThrows(TransferFailedException::class.java) {
            DepositRequest(1L, null)
        }
    }

    @Test
    fun `금액이 0 이하이면 예외가 발생한다`() {
        assertThrows(TransferFailedException::class.java) {
            DepositRequest(1L, BigDecimal.ZERO)
        }
        assertThrows(TransferFailedException::class.java) {
            DepositRequest(1L, BigDecimal("-0.01"))
        }
    }

    @Test
    fun `계좌 아이디가 없으면 예외가 발생한다`() {
        assertThrows(TransferFailedException::class.java) {
            DepositRequest(null, BigDecimal.ONE)
        }
    }
}
