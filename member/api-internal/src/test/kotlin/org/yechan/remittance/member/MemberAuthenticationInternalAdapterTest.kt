package org.yechan.remittance.member.internal.adapter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.yechan.remittance.member.MemberAuthenticationQueryUseCase
import org.yechan.remittance.member.MemberAuthenticationResult
import org.yechan.remittance.member.MemberLoginProps
import org.yechan.remittance.member.internal.contract.MemberAuthenticationRequest
import java.util.concurrent.atomic.AtomicReference

class MemberAuthenticationInternalAdapterTest {
    @Test
    fun `검증 요청은 MemberAuthenticationQueryUseCase에 위임한다`() {
        val captured = AtomicReference<MemberLoginProps>()
        val useCase = MemberAuthenticationQueryUseCase { props ->
            captured.set(props)
            MemberAuthenticationResult(valid = true, memberId = 42L)
        }
        val controller = MemberAuthenticationInternalAdapter(useCase)

        val response = controller.verify(MemberAuthenticationRequest("user@example.com", "pass"))

        assertTrue(response.valid)
        assertEquals(42L, response.memberId)
        assertEquals("user@example.com", captured.get().email)
        assertEquals("pass", captured.get().password)
    }
}
