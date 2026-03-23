package org.yechan.remittance.api.idempotency

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.client.RestTestClient
import org.yechan.remittance.AggregateApplication
import org.yechan.remittance.EmailGenerator
import org.yechan.remittance.IntegrationTestEnvironmentSetup
import org.yechan.remittance.PasswordGenerator
import org.yechan.remittance.TokenVerifier
import org.yechan.remittance.member.dto.MemberLoginRequest
import org.yechan.remittance.member.dto.MemberLoginResponse
import org.yechan.remittance.member.dto.MemberRegisterRequest
import org.yechan.remittance.transfer.dto.IdempotencyKeyCreateResponse
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest(classes = [AggregateApplication::class])
class PostSpecs : IntegrationTestEnvironmentSetup() {
    @Autowired
    lateinit var restTestClient: RestTestClient

    @Autowired
    lateinit var tokenVerifier: TokenVerifier

    @Test
    fun `멱등키 발급 요청은 새로운 키와 만료 시간을 반환한다`() {
        val email = EmailGenerator.generate()
        val password = PasswordGenerator.generate()

        restTestClient.post()
            .uri("/members")
            .body(MemberRegisterRequest("test", email, password))
            .exchange()
            .expectStatus().isOk

        val loginResponse = restTestClient.post()
            .uri("/login")
            .body(MemberLoginRequest(email, password))
            .exchange()
            .expectStatus().isOk
            .expectBody(MemberLoginResponse::class.java)
            .returnResult()
            .responseBody

        assertThat(loginResponse).isNotNull
        assertThat(loginResponse!!.accessToken).isNotBlank()
        val before = LocalDateTime.now()

        val authentication = tokenVerifier.verify(loginResponse.accessToken)
        SecurityContextHolder.getContext().authentication = authentication

        val firstResponse: IdempotencyKeyCreateResponse
        val secondResponse: IdempotencyKeyCreateResponse
        try {
            firstResponse = restTestClient.post()
                .uri("/idempotency-keys")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${loginResponse.accessToken}")
                .exchange()
                .expectStatus().isOk
                .expectBody(IdempotencyKeyCreateResponse::class.java)
                .returnResult()
                .responseBody ?: throw IllegalStateException("First response is null")

            secondResponse = restTestClient.post()
                .uri("/idempotency-keys")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${loginResponse.accessToken}")
                .exchange()
                .expectStatus().isOk
                .expectBody(IdempotencyKeyCreateResponse::class.java)
                .returnResult()
                .responseBody ?: throw IllegalStateException("Second response is null")
        } finally {
            SecurityContextHolder.clearContext()
        }

        assertThat(firstResponse.idempotencyKey).isNotBlank()
        assertThat(secondResponse.idempotencyKey).isNotBlank()
        assertThat(UUID.fromString(firstResponse.idempotencyKey)).isNotNull()
        assertThat(UUID.fromString(secondResponse.idempotencyKey)).isNotNull()
        assertThat(firstResponse.expiresAt).isAfter(before)
        assertThat(secondResponse.expiresAt).isAfter(before)
        assertThat(firstResponse.idempotencyKey).isNotEqualTo(secondResponse.idempotencyKey)
    }
}
