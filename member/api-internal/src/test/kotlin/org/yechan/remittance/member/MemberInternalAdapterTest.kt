package org.yechan.remittance.member.internal.adapter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.yechan.remittance.member.MemberAuthQueryUseCase
import org.yechan.remittance.member.MemberAuthValue
import org.yechan.remittance.member.MemberLoginProps
import org.yechan.remittance.member.internal.contract.LoginVerifyRequest
import java.util.concurrent.atomic.AtomicReference

class MemberInternalAdapterTest {
    @Test
    fun `검증 요청은 MemberAuthQueryUseCase에 위임한다`() {
        val captured = AtomicReference<MemberLoginProps>()
        val useCase = MemberAuthQueryUseCase { props ->
            captured.set(props)
            MemberAuthValue(valid = true, memberId = 42L)
        }
        val controller = MemberInternalAdapter(useCase)

        val response = controller.verify(LoginVerifyRequest("user@example.com", "pass"))

        assertTrue(response.valid)
        assertEquals(42L, response.memberId)
        assertEquals("user@example.com", captured.get().email)
        assertEquals("pass", captured.get().password)
    }
}
