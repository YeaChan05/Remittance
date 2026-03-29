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

@SpringBootTest(
    classes = [AccountApiApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
class PostSpecs {
    @Autowired
    lateinit var restTestClient: RestTestClient

    @Autowired
    lateinit var tokenGenerator: TokenGenerator

    @Autowired
    lateinit var tokenVerifier: TokenVerifier

    @Test
    fun `올바른 계좌 생성 요청은 200 SUCCESS와 계좌 정보를 반환한다`() {
        val token = tokenGenerator.generate(1L).accessToken
        SecurityContextHolder.getContext().authentication = tokenVerifier.verify(token)
        val request = AccountCreateRequest("090", "123-456", "sample-account")

        try {
            val response = restTestClient.post()
                .uri("/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .body(request)
                .exchange()
                .expectStatus().isOk
                .expectBody<AccountCreateResponse>()
                .returnResult()
                .responseBody

            assertThat(response).isNotNull
            assertThat(response!!.accountId).isNotNull()
            assertThat(response.accountName).isEqualTo("sample-account")
        } finally {
            SecurityContextHolder.clearContext()
        }
    }
}
