package org.yechan.remittance.member

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.yechan.remittance.PasswordHashEncoder

class MemberAuthQueryServiceTest {
    @Test
    fun `회원이 존재하지 않으면 유효하지 않은 인증 결과를 반환한다`() {
        val memberRepository = object : MemberRepository {
            override fun save(props: MemberProps): MemberModel = throw UnsupportedOperationException()

            override fun findById(identifier: MemberIdentifier): MemberModel? = null

            override fun findByEmail(email: String): MemberModel? = null
        }
        val passwordHashEncoder = object : PasswordHashEncoder {
            override fun encode(password: String): String = "encoded"

            override fun matches(password: String, encodedPassword: String): Boolean = true
        }
        val useCase = MemberAuthQueryService(memberRepository, passwordHashEncoder)

        val result = useCase.verify(TestLoginProps())

        assertThat(result.valid).isFalse()
        assertThat(result.memberId).isEqualTo(0L)
    }

    @Test
    fun `비밀번호가 일치하지 않으면 유효하지 않은 인증 결과를 반환한다`() {
        val memberRepository = object : MemberRepository {
            override fun save(props: MemberProps): MemberModel = throw UnsupportedOperationException()

            override fun findById(identifier: MemberIdentifier): MemberModel? = null

            override fun findByEmail(email: String): MemberModel? = TestMember(memberId = 7L, password = "hashed")
        }
        val passwordHashEncoder = object : PasswordHashEncoder {
            override fun encode(password: String): String = "encoded"

            override fun matches(password: String, encodedPassword: String): Boolean = false
        }
        val useCase = MemberAuthQueryService(memberRepository, passwordHashEncoder)

        val result = useCase.verify(TestLoginProps())

        assertThat(result.valid).isFalse()
        assertThat(result.memberId).isEqualTo(0L)
    }

    @Test
    fun `회원이 존재하고 비밀번호가 일치하면 유효한 인증 결과를 반환한다`() {
        val memberRepository = object : MemberRepository {
            override fun save(props: MemberProps): MemberModel = throw UnsupportedOperationException()

            override fun findById(identifier: MemberIdentifier): MemberModel? = null

            override fun findByEmail(email: String): MemberModel? = TestMember(memberId = 9L, password = "hashed")
        }
        val passwordHashEncoder = object : PasswordHashEncoder {
            override fun encode(password: String): String = "encoded"

            override fun matches(password: String, encodedPassword: String): Boolean = true
        }
        val useCase = MemberAuthQueryService(memberRepository, passwordHashEncoder)

        val result = useCase.verify(TestLoginProps())

        assertThat(result.valid).isTrue()
        assertThat(result.memberId).isEqualTo(9L)
    }

    private class TestLoginProps : MemberLoginProps {
        override val email: String = "user@example.com"
        override val password: String = "password!1"
    }

    private data class TestMember(
        override val memberId: Long?,
        override val password: String,
    ) : MemberModel {
        override val name: String = "name"
        override val email: String = "user@example.com"
    }
}
