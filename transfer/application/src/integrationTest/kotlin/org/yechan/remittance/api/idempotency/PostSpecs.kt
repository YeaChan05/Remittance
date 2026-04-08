package org.yechan.remittance.api.idempotency

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.expectBody
import org.yechan.remittance.TokenGenerator
import org.yechan.remittance.TokenVerifier
import org.yechan.remittance.transfer.TransferApiApplication
import org.yechan.remittance.transfer.config.TransferInternalApiStubSupport
import org.yechan.remittance.transfer.dto.IdempotencyKeyCreateResponse
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest(
    classes = [TransferApiApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
class PostSpecs : TransferInternalApiStubSupport() {
    @Autowired
    lateinit var restTestClient: RestTestClient

    @Autowired
    lateinit var tokenGenerator: TokenGenerator

    @Autowired
    lateinit var tokenVerifier: TokenVerifier

    @Test
    fun `멱등키 발급 요청은 새로운 키와 만료 시간을 반환한다`() {
        val accessToken = tokenGenerator.generate(1L).accessToken
        SecurityContextHolder.getContext().authentication = tokenVerifier.verify(accessToken)
        val before = LocalDateTime.now()

        try {
            val firstResponse: IdempotencyKeyCreateResponse = restTestClient.post()
                .uri("/idempotency-keys")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .exchange()
                .expectStatus().isOk
                .expectBody<IdempotencyKeyCreateResponse>()
                .returnResult()
                .responseBody ?: throw IllegalStateException("First response is null")

            val secondResponse: IdempotencyKeyCreateResponse = restTestClient.post()
                .uri("/idempotency-keys")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .exchange()
                .expectStatus().isOk
                .expectBody<IdempotencyKeyCreateResponse>()
                .returnResult()
                .responseBody ?: throw IllegalStateException("Second response is null")

            assertThat(firstResponse.idempotencyKey).isNotBlank()
            assertThat(secondResponse.idempotencyKey).isNotBlank()
            assertThat(UUID.fromString(firstResponse.idempotencyKey)).isNotNull()
            assertThat(UUID.fromString(secondResponse.idempotencyKey)).isNotNull()
            assertThat(firstResponse.expiresAt).isAfter(before)
            assertThat(secondResponse.expiresAt).isAfter(before)
            assertThat(firstResponse.idempotencyKey).isNotEqualTo(secondResponse.idempotencyKey)
        } finally {
            SecurityContextHolder.clearContext()
        }
    }
}
