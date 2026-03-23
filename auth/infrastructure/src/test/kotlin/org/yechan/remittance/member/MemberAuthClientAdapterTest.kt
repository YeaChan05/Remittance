package org.yechan.remittance.member

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.yechan.remittance.auth.MemberAuthClientAdapter
import org.yechan.remittance.member.internal.contract.LoginVerifyRequest
import org.yechan.remittance.member.internal.contract.LoginVerifyResponse
import org.yechan.remittance.member.internal.contract.MemberInternalApi
import java.util.concurrent.atomic.AtomicReference

class MemberAuthClientAdapterTest {
    @Test
    fun `verify delegates to member internal api`() {
        val captured = AtomicReference<LoginVerifyRequest>()
        val memberInternalApi = MemberInternalApi { request ->
            captured.set(request)
            LoginVerifyResponse(true, 7L)
        }
        val adapter = MemberAuthClientAdapter(memberInternalApi)

        val result = adapter.verify("user@example.com", "secret")

        assertTrue(result.valid)
        assertEquals(7L, result.memberId)
        assertEquals("user@example.com", captured.get().email)
        assertEquals("secret", captured.get().password)
    }
}
