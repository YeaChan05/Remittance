package org.yechan.remittance.api.account

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
import org.yechan.remittance.account.AccountApiApplication
import org.yechan.remittance.account.dto.AccountCreateRequest
import org.yechan.remittance.account.dto.AccountCreateResponse
import org.yechan.remittance.account.dto.AccountDeleteResponse

@SpringBootTest(
    classes = [AccountApiApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
class DeleteSpecs {
    @Autowired
    lateinit var restTestClient: RestTestClient

    @Autowired
    lateinit var tokenGenerator: TokenGenerator

    @Autowired
    lateinit var tokenVerifier: TokenVerifier

    @Test
    fun `올바른 계좌 삭제 요청은 200 SUCCESS와 삭제된 계좌 id를 반환한다`() {
        val token = tokenGenerator.generate(1L).accessToken
        SecurityContextHolder.getContext().authentication = tokenVerifier.verify(token)

        try {
            val created = restTestClient.post()
                .uri("/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .body(AccountCreateRequest("090", "123-456", "sample-account"))
                .exchange()
                .expectStatus().isOk
                .expectBody<AccountCreateResponse>()
                .returnResult()
                .responseBody

            assertThat(created).isNotNull

            val response = restTestClient.delete()
                .uri("/accounts/{accountId}", created!!.accountId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .exchange()
                .expectStatus().isOk
                .expectBody<AccountDeleteResponse>()
                .returnResult()
                .responseBody

            assertThat(response).isNotNull
            assertThat(response!!.accountId).isEqualTo(created.accountId)
        } finally {
            SecurityContextHolder.clearContext()
        }
    }
}
