package org.yechan.remittance.api.account

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
import org.yechan.remittance.account.dto.AccountCreateRequest
import org.yechan.remittance.account.dto.AccountCreateResponse
import org.yechan.remittance.account.dto.AccountDeleteResponse
import org.yechan.remittance.member.dto.MemberLoginRequest
import org.yechan.remittance.member.dto.MemberLoginResponse
import org.yechan.remittance.member.dto.MemberRegisterRequest

@SpringBootTest(classes = [AggregateApplication::class])
class DeleteSpecs : IntegrationTestEnvironmentSetup() {
    @Autowired
    lateinit var restTestClient: RestTestClient

    @Autowired
    lateinit var tokenVerifier: TokenVerifier

    @Test
    fun `올바른 계좌 삭제 요청은 200 SUCCESS와 삭제된 계좌 id를 반환한다`() {
        val token = login()
        val authentication = tokenVerifier.verify(token)
        SecurityContextHolder.getContext().authentication = authentication

        try {
            val created = restTestClient.post()
                .uri("/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .body(AccountCreateRequest("090", "123-456", "sample-account"))
                .exchange()
                .expectStatus().isOk
                .expectBody(AccountCreateResponse::class.java)
                .returnResult()
                .responseBody

            assertThat(created).isNotNull

            val response = restTestClient.delete()
                .uri("/accounts/{accountId}", created!!.accountId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .exchange()
                .expectStatus().isOk
                .expectBody(AccountDeleteResponse::class.java)
                .returnResult()
                .responseBody

            assertThat(response).isNotNull
            assertThat(response!!.accountId).isEqualTo(created.accountId)
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    private fun login(): String {
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
        return loginResponse.accessToken
    }
}
