package org.yechan.remittance.api.member

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
import org.yechan.remittance.member.dto.MemberRegisterRequest
import org.yechan.remittance.member.dto.MemberRegisterResponse

@SpringBootTest(classes = [AggregateApplication::class])
class PostSpecs : IntegrationTestEnvironmentSetup() {
    @Autowired
    lateinit var restTestClient: RestTestClient

    @Test
    fun `올바른 회원가입 요청은 200 SUCCESS와 회원 정보를 반환한다`() {
        val name = "test"
        val request = MemberRegisterRequest(name, EmailGenerator.generate(), PasswordGenerator.generate())

        val response = restTestClient.post()
            .uri("/members")
            .body(request)
            .exchange()
            .expectStatus().isOk
            .expectBody(MemberRegisterResponse::class.java)
            .returnResult()
            .responseBody

        assertThat(response).isNotNull
        assertThat(response!!.name).isEqualTo(name)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "test@", "test@test"])
    fun `잘못된 이메일 형식의 회원가입 요청은 400 BAD REQUEST를 반환한다`(email: String) {
        val request = MemberRegisterRequest("test", email, PasswordGenerator.generate())

        restTestClient.post()
            .uri("/members")
            .body(request)
            .exchange()
            .expectStatus().isBadRequest
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "pswd1!", "password", "password1", "password!", "12345678!"])
    fun `잘못된 비밀번호 형식의 회원가입 요청은 400 BAD REQUEST를 반환한다`(password: String) {
        val request = MemberRegisterRequest("test", EmailGenerator.generate(), password)

        restTestClient.post()
            .uri("/members")
            .body(request)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `중복 이메일로 회원가입하면 서버 오류 응답을 반환한다`() {
        val email = EmailGenerator.generate()
        val password = PasswordGenerator.generate()

        restTestClient.post()
            .uri("/members")
            .body(MemberRegisterRequest("test", email, password))
            .exchange()
            .expectStatus().isOk

        restTestClient.post()
            .uri("/members")
            .body(MemberRegisterRequest("test", email, password))
            .exchange()
            .expectStatus().is5xxServerError
            .expectBody(String::class.java)
            .consumeWith { res ->
                assertThat(requireNotNull(res.responseBody)).contains("Email already exists:")
            }
    }
}
