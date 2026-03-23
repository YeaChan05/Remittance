package org.yechan.remittance.member.internal.adapter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.yechan.remittance.member.MemberExistenceQueryUseCase
import org.yechan.remittance.member.internal.contract.MemberExistsRequest
import java.util.concurrent.atomic.AtomicLong

class MemberExistenceInternalAdapterTest {
    @Test
    fun `회원 존재 확인 요청은 MemberExistenceQueryUseCase에 위임한다`() {
        val captured = AtomicLong()
        val useCase = MemberExistenceQueryUseCase { memberId ->
            captured.set(memberId)
            true
        }
        val adapter = MemberExistenceInternalAdapter(useCase)

        val response = adapter.exists(MemberExistsRequest(42L))

        assertThat(response.exists).isTrue()
        assertThat(captured.get()).isEqualTo(42L)
    }
}
