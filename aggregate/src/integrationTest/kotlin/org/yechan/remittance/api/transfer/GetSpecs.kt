package org.yechan.remittance.api.transfer

import jakarta.persistence.EntityManager
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.transaction.support.TransactionTemplate
import org.yechan.remittance.AggregateApplication
import org.yechan.remittance.IntegrationTestEnvironmentSetup
import org.yechan.remittance.TransferTestFixtures
import org.yechan.remittance.TransferTestFixturesConfig
import org.yechan.remittance.transfer.dto.IdempotencyKeyCreateResponse
import org.yechan.remittance.transfer.dto.TransferQueryResponse
import org.yechan.remittance.transfer.dto.TransferRequest

@SpringBootTest(classes = [AggregateApplication::class])
@Import(TransferTestFixturesConfig::class)
class GetSpecs : IntegrationTestEnvironmentSetup() {
    @Autowired
    lateinit var restTestClient: RestTestClient

    @Autowired
    lateinit var fixtures: TransferTestFixtures

    @Autowired
    lateinit var entityManager: EntityManager

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `이체 내역은 completedAt 내림차순으로 조회된다`() {
        val result = fixtures.setupAuthentication()
        val memberId = result.authentication.name.toLong()
        val fromAccount = fixtures.createAccountWithBalance(memberId, "출금", BigDecimal.valueOf(5_000_000L))
        val toAccount = fixtures.createAccountWithBalance(memberId, "입금", BigDecimal.ZERO)

        val firstKey = issueIdempotencyKey(result.auth.accessToken)
        val firstResponse = transfer(
            result.auth.accessToken,
            firstKey,
            fromAccount.accountId,
            toAccount.accountId,
            BigDecimal.valueOf(100_000L)
        )

        val secondKey = issueIdempotencyKey(result.auth.accessToken)
        val secondResponse = transfer(
            result.auth.accessToken,
            secondKey,
            fromAccount.accountId,
            toAccount.accountId,
            BigDecimal.valueOf(200_000L)
        )

        val older = LocalDateTime.now().minusHours(2).truncatedTo(ChronoUnit.MICROS)
        val newer = LocalDateTime.now().minusHours(1).truncatedTo(ChronoUnit.MICROS)
        updateCompletedAt(requireNotNull(firstResponse.transferId), older)
        updateCompletedAt(requireNotNull(secondResponse.transferId), newer)

        val response = query(result.auth.accessToken, fromAccount.accountId, null, null, null)

        assertThat(response.transfers).hasSize(2)
        assertThat(response.transfers.first().transferId).isEqualTo(secondResponse.transferId)
        assertThat(response.transfers[1].transferId).isEqualTo(firstResponse.transferId)
        assertThat(response.transfers.map { it.completedAt }).containsExactly(newer, older)
    }

    @Test
    fun `입금 받은 계좌에서도 이체 내역이 조회된다`() {
        val result = fixtures.setupAuthentication()
        val memberId = result.authentication.name.toLong()
        val fromAccount = fixtures.createAccountWithBalance(memberId, "출금", BigDecimal.valueOf(1_000_000L))
        val toAccount = fixtures.createAccountWithBalance(memberId, "입금", BigDecimal.ZERO)

        val key = issueIdempotencyKey(result.auth.accessToken)
        val response = transfer(
            result.auth.accessToken,
            key,
            fromAccount.accountId,
            toAccount.accountId,
            BigDecimal.valueOf(100_000L)
        )

        val queryResponse = query(result.auth.accessToken, toAccount.accountId, null, null, null)

        assertThat(queryResponse.transfers).hasSize(1)
        assertThat(queryResponse.transfers.first().transferId).isEqualTo(response.transferId)
        assertThat(queryResponse.transfers.first().fromAccountId).isEqualTo(fromAccount.accountId)
        assertThat(queryResponse.transfers.first().toAccountId).isEqualTo(toAccount.accountId)
    }

    @Test
    fun `조회 limit을 적용한다`() {
        val result = fixtures.setupAuthentication()
        val memberId = result.authentication.name.toLong()
        val fromAccount = fixtures.createAccountWithBalance(memberId, "출금", BigDecimal.valueOf(2_000_000L))
        val toAccount = fixtures.createAccountWithBalance(memberId, "입금", BigDecimal.ZERO)

        val firstKey = issueIdempotencyKey(result.auth.accessToken)
        transfer(
            result.auth.accessToken,
            firstKey,
            fromAccount.accountId,
            toAccount.accountId,
            BigDecimal.valueOf(100_000L)
        )

        val secondKey = issueIdempotencyKey(result.auth.accessToken)
        transfer(
            result.auth.accessToken,
            secondKey,
            fromAccount.accountId,
            toAccount.accountId,
            BigDecimal.valueOf(200_000L)
        )

        val response = query(result.auth.accessToken, fromAccount.accountId, null, null, 1)
        assertThat(response.transfers).hasSize(1)
    }

    private fun query(
        accessToken: String,
        accountId: Long,
        from: LocalDateTime?,
        to: LocalDateTime?,
        limit: Int?
    ): TransferQueryResponse {
        return restTestClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("/transfers")
                    .queryParam("accountId", accountId)
                    .queryParamIfPresent("from", Optional.ofNullable(from))
                    .queryParamIfPresent("to", Optional.ofNullable(to))
                    .queryParamIfPresent("limit", Optional.ofNullable(limit))
                    .build()
            }
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(TransferQueryResponse::class.java)
            .returnResult()
            .responseBody ?: throw IllegalStateException("Transfer query response is null")
    }

    private fun transfer(
        accessToken: String,
        idempotencyKey: String,
        fromAccountId: Long,
        toAccountId: Long,
        amount: BigDecimal
    ): TransferResponse {
        return restTestClient.post()
            .uri { uriBuilder -> uriBuilder.path("/transfers/$idempotencyKey").build() }
            .body(TransferRequest(fromAccountId, toAccountId, amount))
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(TransferResponse::class.java)
            .returnResult()
            .responseBody ?: throw IllegalStateException("Transfer response is null")
    }

    private fun issueIdempotencyKey(accessToken: String): String {
        val response = restTestClient.post()
            .uri("/idempotency-keys")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(IdempotencyKeyCreateResponse::class.java)
            .returnResult()
            .responseBody ?: throw IllegalStateException("Idempotency key response is null")

        return response.idempotencyKey
    }

    private fun updateCompletedAt(
        transferId: Long,
        completedAt: LocalDateTime
    ) {
        transactionTemplate.executeWithoutResult {
            entityManager.createQuery(
                """
                    update TransferEntity t
                       set t.completedAt = :completedAt
                     where t.id = :transferId
                """.trimIndent()
            )
                .setParameter("completedAt", completedAt)
                .setParameter("transferId", transferId)
                .executeUpdate()
            entityManager.flush()
        }
    }

    data class TransferResponse(
        val status: String,
        val transferId: Long?,
        val errorCode: String?
    )
}
