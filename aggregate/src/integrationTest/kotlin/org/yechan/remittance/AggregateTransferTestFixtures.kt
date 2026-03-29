package org.yechan.remittance

import jakarta.persistence.EntityManager
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.expectBody
import org.springframework.transaction.support.TransactionTemplate
import org.yechan.remittance.account.dto.AccountCreateRequest
import org.yechan.remittance.account.dto.AccountCreateResponse
import org.yechan.remittance.member.dto.MemberLoginResponse
import org.yechan.remittance.transfer.dto.IdempotencyKeyCreateResponse
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger

class AggregateTransferTestFixtures(
    private val restTestClient: RestTestClient,
    private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
    private val tokenVerifier: TokenVerifier,
) {
    private val nextAccountNumber = AtomicInteger(1)

    fun reset() {
        nextAccountNumber.set(1)
        transactionTemplate.executeWithoutResult {
            entityManager.createQuery("delete from LedgerEntity").executeUpdate()
            entityManager.createQuery("delete from OutboxEventEntity").executeUpdate()
            entityManager.createQuery("delete from TransferEntity").executeUpdate()
            entityManager.createQuery("delete from IdempotencyKeyEntity").executeUpdate()
            entityManager.createQuery("delete from DailyLimitUsageEntity").executeUpdate()
            entityManager.createQuery("delete from ProcessedEventEntity").executeUpdate()
            entityManager.createQuery("delete from AccountEntity").executeUpdate()
            entityManager.createQuery("delete from MemberEntity").executeUpdate()
            entityManager.flush()
            entityManager.clear()
        }
    }

    fun registerMember(
        name: String,
        email: String,
        password: String,
    ) {
        restTestClient.post()
            .uri("/members")
            .body(
                mapOf(
                    "name" to name,
                    "email" to email,
                    "password" to password,
                ),
            )
            .exchange()
            .expectStatus().isOk
    }

    fun login(
        email: String,
        password: String,
    ): AuthInfo {
        val response = restTestClient.post()
            .uri("/login")
            .body(
                mapOf(
                    "email" to email,
                    "password" to password,
                ),
            )
            .exchange()
            .expectStatus().isOk
            .expectBody<MemberLoginResponse>()
            .returnResult()
            .responseBody ?: throw IllegalStateException("Member login response is null")

        val memberId = tokenVerifier.verify(response.accessToken).name.toLong()
        return AuthInfo(memberId, response.accessToken, response.refreshToken)
    }

    fun createAccount(
        accessToken: String,
        accountName: String,
    ): AccountInfo {
        val accountNumber = nextAccountNumber.getAndIncrement().toString().padStart(12, '0')
        val response = withAuthentication(accessToken) {
            restTestClient.post()
                .uri("/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .body(AccountCreateRequest("090", accountNumber, accountName))
                .exchange()
                .expectStatus().isOk
                .expectBody<AccountCreateResponse>()
                .returnResult()
                .responseBody ?: throw IllegalStateException("Account create response is null")
        }

        return AccountInfo(requireNotNull(response.accountId), response.accountName)
    }

    fun issueTransferIdempotencyKeys(
        accessToken: String,
        count: Int,
    ): List<String> = (0 until count).map {
        val response = withAuthentication(accessToken) {
            restTestClient.post()
                .uri("/idempotency-keys?scope=TRANSFER")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .exchange()
                .expectStatus().isOk
                .expectBody<IdempotencyKeyCreateResponse>()
                .returnResult()
                .responseBody ?: throw IllegalStateException("Idempotency key response is null")
        }
        response.idempotencyKey
    }

    fun deposit(
        accessToken: String,
        accountId: Long,
        amount: BigDecimal,
    ): TransferResponse {
        val key = issueDepositIdempotencyKey(accessToken)
        return withAuthentication(accessToken) {
            restTestClient.post()
                .uri("/deposits/$key")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .body(
                    mapOf(
                        "accountId" to accountId,
                        "amount" to amount,
                    ),
                )
                .exchange()
                .expectStatus().isOk
                .expectBody<TransferResponse>()
                .returnResult()
                .responseBody ?: throw IllegalStateException("Deposit response is null")
        }
    }

    fun transfer(
        accessToken: String,
        idempotencyKey: String,
        fromAccountId: Long,
        toAccountId: Long,
        amount: BigDecimal,
    ): TransferResponse = withAuthentication(accessToken) {
        restTestClient.post()
            .uri("/transfers/$idempotencyKey")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .body(
                mapOf(
                    "fromAccountId" to fromAccountId,
                    "toAccountId" to toAccountId,
                    "amount" to amount,
                ),
            )
            .exchange()
            .expectStatus().isOk
            .expectBody<TransferResponse>()
            .returnResult()
            .responseBody ?: throw IllegalStateException("Transfer response is null")
    }

    fun loadAccountBalance(accountId: Long): BigDecimal = entityManager.createQuery(
        """
            select a.balance
              from AccountEntity a
             where a.id = :accountId
        """.trimIndent(),
        BigDecimal::class.java,
    )
        .setParameter("accountId", accountId)
        .singleResult

    fun countTransfers(): Long = entityManager.createQuery(
        "select count(t) from TransferEntity t",
        java.lang.Long::class.java,
    ).singleResult.toLong()

    fun countLedgers(): Long = entityManager.createQuery(
        "select count(l) from LedgerEntity l",
        java.lang.Long::class.java,
    ).singleResult.toLong()

    fun countOutboxEvents(): Long = entityManager.createQuery(
        "select count(o) from OutboxEventEntity o",
        java.lang.Long::class.java,
    ).singleResult.toLong()

    fun countSentOutboxEvents(): Long = entityManager.createQuery(
        """
            select count(o)
              from OutboxEventEntity o
             where o.status = org.yechan.remittance.transfer.OutboxEventProps.OutboxEventStatusValue.SENT
        """.trimIndent(),
        java.lang.Long::class.java,
    )
        .singleResult
        .toLong()

    fun countProcessedEvents(): Long = entityManager.createQuery(
        "select count(p) from ProcessedEventEntity p",
        java.lang.Long::class.java,
    ).singleResult.toLong()

    private fun issueDepositIdempotencyKey(accessToken: String): String {
        val response = withAuthentication(accessToken) {
            restTestClient.post()
                .uri("/idempotency-keys?scope=DEPOSIT")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .exchange()
                .expectStatus().isOk
                .expectBody<IdempotencyKeyCreateResponse>()
                .returnResult()
                .responseBody
                ?: throw IllegalStateException("Deposit idempotency key response is null")
        }

        return response.idempotencyKey
    }

    private fun <T> withAuthentication(
        accessToken: String,
        block: () -> T,
    ): T {
        SecurityContextHolder.getContext().authentication = tokenVerifier.verify(accessToken)
        return try {
            block()
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    data class AuthInfo(
        val memberId: Long,
        val accessToken: String,
        val refreshToken: String,
    )

    data class AccountInfo(
        val accountId: Long,
        val accountName: String,
    )

    data class TransferResponse(
        val status: String,
        val transferId: Long?,
        val errorCode: String?,
    )
}
