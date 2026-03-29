package org.yechan.remittance.member

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.yechan.remittance.AuthTokenValue
import org.yechan.remittance.PasswordHashEncoder
import org.yechan.remittance.TokenGenerator

class MemberQueryServiceTest {
    @Test
    fun `올바른 인증 정보는 인증 토큰을 반환한다`() {
        val memberRepository = object : MemberRepository {
            override fun save(props: MemberProps): MemberModel = throw UnsupportedOperationException()

            override fun findById(identifier: MemberIdentifier): MemberModel? = null

            override fun findByEmail(email: String): MemberModel? = TestMember(memberId = 1L, password = "hashed")
        }
        val passwordHashEncoder = object : PasswordHashEncoder {
            override fun encode(password: String): String = "encoded"

            override fun matches(password: String, encodedPassword: String): Boolean = true
        }
        val tokenGenerator = TokenGenerator { AuthTokenValue("access", "refresh", 3600L) }
        val useCase: MemberQueryUseCase =
            MemberQueryService(memberRepository, passwordHashEncoder, tokenGenerator)

        val token = useCase.login(TestLoginProps())

        assertThat(token.accessToken).isEqualTo("access")
        assertThat(token.refreshToken).isEqualTo("refresh")
        assertThat(token.expiresIn).isEqualTo(3600L)
    }

    @Test
    fun `올바르지 않은 인증 정보는 인증 예외를 던진다`() {
        val memberRepository = object : MemberRepository {
            override fun save(props: MemberProps): MemberModel = throw UnsupportedOperationException()

            override fun findById(identifier: MemberIdentifier): MemberModel? = null

            override fun findByEmail(email: String): MemberModel? = TestMember(memberId = 1L, password = "hashed")
        }
        val passwordHashEncoder = object : PasswordHashEncoder {
            override fun encode(password: String): String = "encoded"

            override fun matches(password: String, encodedPassword: String): Boolean = false
        }
        val tokenGenerator = TokenGenerator { AuthTokenValue("access", "refresh", 3600L) }
        val useCase: MemberQueryUseCase =
            MemberQueryService(memberRepository, passwordHashEncoder, tokenGenerator)

        assertThatThrownBy { useCase.login(TestLoginProps()) }
            .isInstanceOf(MemberPermissionDeniedException::class.java)
            .hasMessage("Invalid credentials")
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
