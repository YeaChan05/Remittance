package org.yechan.remittance.transfer

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TransferSnapshotUtilTest {
    @Test
    fun `스냅샷은 직렬화와 역직렬화를 왕복한다`() {
        val util = TransferSnapshotUtil(ObjectMapper())
        val result = TransferResult(TransferProps.TransferStatusValue.SUCCEEDED, 10L, null)

        val snapshot = util.toSnapshot(result)
        val restored = util.fromSnapshot(snapshot)

        assertEquals(result.status, restored.status)
        assertEquals(result.transferId, restored.transferId)
        assertEquals(result.errorCode, restored.errorCode)
    }

    @Test
    fun `비어 있는 스냅샷은 거부한다`() {
        val util = TransferSnapshotUtil(ObjectMapper())

        assertThrows(IllegalArgumentException::class.java) { util.fromSnapshot("") }
        assertThrows(IllegalArgumentException::class.java) { util.fromSnapshot("   ") }
    }

    @Test
    fun `잘못된 JSON 스냅샷은 예외가 발생한다`() {
        val util = TransferSnapshotUtil(ObjectMapper())

        assertThrows(TransferException::class.java) { util.fromSnapshot("not-json") }
    }

    @Test
    fun `직렬화에 실패하면 도메인 예외로 변환한다`() {
        val failingMapper = object : ObjectMapper() {
            override fun writeValueAsString(value: Any?): String {
                throw object : JsonProcessingException("fail") {}
            }
        }
        val util = TransferSnapshotUtil(failingMapper)

        assertThrows(TransferException::class.java) {
            util.toHashRequest(TestRequestProps())
        }
    }

    @Test
    fun `요청 해시는 결정적으로 생성된다`() {
        val util = TransferSnapshotUtil(ObjectMapper())
        val props: TransferRequestProps = TestRequestProps()

        val hash1 = util.toHashRequest(props)
        val hash2 = util.toHashRequest(props)

        assertEquals(hash1, hash2)
        assertTrue(hash1.isNotEmpty())
    }

    private class TestRequestProps : TransferRequestProps {
        override val fromAccountId: Long = 1L
        override val toAccountId: Long = 2L
        override val amount: BigDecimal = BigDecimal("100.00")
        override val scope: TransferProps.TransferScopeValue = TransferProps.TransferScopeValue.TRANSFER
        override val fee: BigDecimal = BigDecimal.ONE
    }
}
