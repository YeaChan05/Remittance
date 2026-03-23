package org.yechan.remittance.account

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.yechan.remittance.account.dto.AccountCreateRequest
import java.math.BigDecimal

class AccountControllerTest {
    @Test
    fun `계좌 생성 요청은 응답 본문에 생성된 계좌를 담아 반환한다`() {
        val createUseCase =
            AccountCreateUseCase { props ->
                Account(
                    11L,
                    props.memberId,
                    props.bankCode,
                    props.accountNumber,
                    props.accountName,
                    props.balance,
                )
            }
        val deleteUseCase =
            AccountDeleteUseCase { props ->
                Account(
                    props.accountId,
                    props.memberId,
                    "090",
                    "123-456",
                    "sample-account",
                    BigDecimal.ZERO,
                )
            }
        val controller = AccountController(createUseCase, deleteUseCase)

        val response = controller.create(1L, AccountCreateRequest("090", "123-456", "sample-account"))

        assertNotNull(response.body)
        assertEquals(11L, response.body!!.accountId)
        assertEquals("sample-account", response.body!!.accountName)
    }

    @Test
    fun `계좌 삭제 요청은 응답 본문에 삭제된 계좌 id를 담아 반환한다`() {
        val createUseCase =
            AccountCreateUseCase { props ->
                Account(
                    11L,
                    props.memberId,
                    props.bankCode,
                    props.accountNumber,
                    props.accountName,
                    props.balance,
                )
            }
        val deleteUseCase =
            AccountDeleteUseCase { props ->
                Account(
                    props.accountId,
                    props.memberId,
                    "090",
                    "123-456",
                    "sample-account",
                    BigDecimal.ZERO,
                )
            }
        val controller = AccountController(createUseCase, deleteUseCase)

        val response = controller.delete(1L, 11L)

        assertNotNull(response.body)
        assertEquals(11L, response.body!!.accountId)
    }
}
