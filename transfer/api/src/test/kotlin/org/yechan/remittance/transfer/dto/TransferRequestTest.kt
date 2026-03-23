package org.yechan.remittance.transfer.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.yechan.remittance.transfer.TransferFailedException
import org.yechan.remittance.transfer.TransferProps
import java.math.BigDecimal

class TransferRequestTest {
    @Test
    fun `유효한 이체 요청을 생성한다`() {
        val request = TransferRequest(1L, 2L, BigDecimal("100.129"))

        assertEquals(TransferProps.TransferScopeValue.TRANSFER, request.scope)
        assertEquals(BigDecimal("1.00"), request.fee)
    }

    @Test
    fun `금액이 없으면 예외가 발생한다`() {
        assertThrows(TransferFailedException::class.java) {
            TransferRequest(1L, 2L, null)
        }
    }

    @Test
    fun `금액이 0 이하이면 예외가 발생한다`() {
        assertThrows(TransferFailedException::class.java) {
            TransferRequest(1L, 2L, BigDecimal.ZERO)
        }
        assertThrows(TransferFailedException::class.java) {
            TransferRequest(1L, 2L, BigDecimal("-1"))
        }
    }

    @Test
    fun `출금 또는 입금 계좌가 없으면 예외가 발생한다`() {
        assertThrows(TransferFailedException::class.java) {
            TransferRequest(null, 2L, BigDecimal.ONE)
        }
        assertThrows(TransferFailedException::class.java) {
            TransferRequest(1L, null, BigDecimal.ONE)
        }
    }
}
