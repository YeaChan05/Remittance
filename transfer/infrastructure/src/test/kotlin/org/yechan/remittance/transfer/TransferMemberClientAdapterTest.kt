package org.yechan.remittance.transfer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.yechan.remittance.member.internal.contract.MemberExistenceInternalApi
import org.yechan.remittance.member.internal.contract.MemberExistsResponse
import java.util.concurrent.atomic.AtomicLong

class TransferMemberClientAdapterTest {
    @Test
    fun `회원 클라이언트는 provider member existence api에 위임한다`() {
        val captured = AtomicLong()
        val memberExistenceInternalApi = MemberExistenceInternalApi { request ->
            captured.set(request.memberId)
            MemberExistsResponse(true)
        }
        val adapter = TransferMemberClientAdapter(memberExistenceInternalApi)

        val result = adapter.exists(7L)

        assertThat(result).isTrue()
        assertThat(captured.get()).isEqualTo(7L)
    }
}
