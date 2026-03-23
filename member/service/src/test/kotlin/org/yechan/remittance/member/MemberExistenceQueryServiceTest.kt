package org.yechan.remittance.member

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MemberExistenceQueryServiceTest {
    @Test
    fun `회원이 없으면 false를 반환한다`() {
        val repository = object : MemberRepository {
            override fun save(props: MemberProps): MemberModel = throw UnsupportedOperationException()

            override fun findById(identifier: MemberIdentifier): MemberModel? = null

            override fun findByEmail(email: String): MemberModel? = throw UnsupportedOperationException()
        }
        val useCase = MemberExistenceQueryService(repository)

        assertThat(useCase.exists(1L)).isFalse()
    }

    @Test
    fun `회원이 있으면 true를 반환한다`() {
        val repository = object : MemberRepository {
            override fun save(props: MemberProps): MemberModel = throw UnsupportedOperationException()

            override fun findById(identifier: MemberIdentifier): MemberModel? = TestMember(requireNotNull(identifier.memberId))

            override fun findByEmail(email: String): MemberModel? = throw UnsupportedOperationException()
        }
        val useCase = MemberExistenceQueryService(repository)

        assertThat(useCase.exists(7L)).isTrue()
    }

    private data class TestMember(
        override val memberId: Long?,
    ) : MemberModel {
        override val name: String = "name"
        override val email: String = "user@example.com"
        override val password: String = "hashed"
    }
}
