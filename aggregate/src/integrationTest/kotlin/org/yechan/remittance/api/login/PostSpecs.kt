package org.yechan.remittance.api.login

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.client.RestTestClient
import org.yechan.remittance.AggregateApplication
import org.yechan.remittance.EmailGenerator
import org.yechan.remittance.PasswordGenerator
import org.yechan.remittance.IntegrationTestEnvironmentSetup
import org.yechan.remittance.member.dto.MemberLoginRequest
import org.yechan.remittance.member.dto.MemberLoginResponse
import org.yechan.remittance.member.dto.MemberRegisterRequest

@SpringBootTest(classes = [AggregateApplication::class])
class PostSpecs : IntegrationTestEnvironmentSetup() {
    @Autowired
    lateinit var restTestClient: RestTestClient

    @Test
    fun `올바른 로그인 요청은 200 SUCCESS와 토큰을 반환한다`() {
        val email = EmailGenerator.generate()
        val password = PasswordGenerator.generate()

        restTestClient.post()
            .uri("/members")
            .body(MemberRegisterRequest("test", email, password))
            .exchange()
            .expectStatus().isOk

        val response = restTestClient.post()
            .uri("/login")
            .body(MemberLoginRequest(email, password))
            .exchange()
            .expectStatus().isOk
            .expectBody(MemberLoginResponse::class.java)
            .returnResult()
            .responseBody

        assertThat(response).isNotNull
        assertThat(response!!.accessToken).isNotBlank()
        assertThat(response.refreshToken).isNotBlank()
        assertThat(response.expiresIn).isPositive()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "test@", "test@test"])
    fun `잘못된 이메일 형식의 로그인 요청은 400 BAD REQUEST를 반환한다`(email: String) {
        val request = MemberLoginRequest(email, PasswordGenerator.generate())

        restTestClient.post()
            .uri("/login")
            .body(request)
            .exchange()
            .expectStatus().isBadRequest
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "pswd1!", "password", "password1", "password!", "12345678!"])
    fun `잘못된 비밀번호 형식의 로그인 요청은 400 BAD REQUEST를 반환한다`(password: String) {
        val request = MemberLoginRequest(EmailGenerator.generate(), password)

        restTestClient.post()
            .uri("/login")
            .body(request)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `유효하지 않은 자격 증명으로 로그인하면 401 UNAUTHORIZED를 반환한다`() {
        val email = EmailGenerator.generate()
        val password = PasswordGenerator.generate()

        restTestClient.post()
            .uri("/members")
            .body(MemberRegisterRequest("test", email, password))
            .exchange()
            .expectStatus().isOk

        restTestClient.post()
            .uri("/login")
            .body(MemberLoginRequest(email, PasswordGenerator.generate()))
            .exchange()
            .expectStatus().isUnauthorized
            .expectBody(String::class.java)
            .consumeWith { res ->
                assertThat(requireNotNull(res.responseBody)).contains("Invalid credentials")
            }
    }
}
