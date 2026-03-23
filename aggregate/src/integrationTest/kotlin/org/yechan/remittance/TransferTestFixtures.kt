package org.yechan.remittance

import jakarta.persistence.EntityManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.transaction.support.TransactionTemplate
import org.yechan.remittance.account.AccountProps
import org.yechan.remittance.member.dto.MemberLoginRequest
import org.yechan.remittance.member.dto.MemberLoginResponse
import org.yechan.remittance.member.dto.MemberRegisterRequest
import org.yechan.remittance.transfer.IdempotencyKeyProps
import java.math.BigDecimal
import java.time.LocalDateTime

class TransferTestFixtures(
    private val restTestClient: RestTestClient,
    private val em: EntityManager,
    private val transactionTemplate: TransactionTemplate,
    private val tokenVerifier: TokenVerifier,
) {
    fun createAccountWithBalance(
        memberId: Long,
        accountName: String,
        balance: BigDecimal,
    ): AccountSeed {
        val account = createAccountEntity(memberId, accountName, balance)
        transactionTemplate.executeWithoutResult {
            em.persist(account)
            em.flush()
        }

        return AccountSeed(
            ReflectionTestUtils.invokeMethod<Long>(account, "getAccountId")
                ?: throw IllegalStateException("Account id is null"),
            ReflectionTestUtils.invokeMethod<String>(account, "getBankCode")
                ?: throw IllegalStateException("Bank code is null"),
            ReflectionTestUtils.invokeMethod<String>(account, "getAccountNumber")
                ?: throw IllegalStateException("Account number is null"),
            ReflectionTestUtils.invokeMethod<String>(account, "getAccountName")
                ?: throw IllegalStateException("Account name is null"),
            ReflectionTestUtils.invokeMethod<BigDecimal>(account, "getBalance")
                ?: throw IllegalStateException("Balance is null"),
        )
    }

    private fun createAccountEntity(
        memberId: Long,
        accountName: String,
        balance: BigDecimal,
    ): Any = try {
        val accountEntityClass = Class.forName("org.yechan.remittance.account.repository.AccountEntity")
        val companion = accountEntityClass.getDeclaredField("Companion").get(null)
        companion.javaClass.getDeclaredMethod("create", AccountProps::class.java)
            .invoke(
                companion,
                object : AccountProps {
                    override val memberId: Long = memberId
                    override val bankCode: String = "001"
                    override val accountNumber: String = System.currentTimeMillis().toString()
                    override val accountName: String = accountName
                    override val balance: BigDecimal = balance
                },
            )
    } catch (exception: ReflectiveOperationException) {
        throw IllegalStateException("AccountEntity create reflection failed", exception)
    }

    fun registerAndIssueToken(name: String): AuthSeed {
        val email = EmailGenerator.generate()
        val password = PasswordGenerator.generate()

        restTestClient.post()
            .uri("/members")
            .body(MemberRegisterRequest(name, email, password))
            .exchange()
            .expectStatus().isOk

        val response = restTestClient.post()
            .uri("/login")
            .body(MemberLoginRequest(email, password))
            .exchange()
            .expectStatus().isOk
            .expectBody(MemberLoginResponse::class.java)
            .returnResult()
            .responseBody ?: throw IllegalStateException("Login response is null")

        return AuthSeed(response.accessToken, email, password)
    }

    fun loadIdempotencyKey(
        memberId: Long,
        idempotencyKey: String,
    ): IdempotencyRow = loadIdempotencyKey(
        memberId,
        idempotencyKey,
        IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
    )

    fun loadIdempotencyKey(
        memberId: Long,
        idempotencyKey: String,
        scope: IdempotencyKeyProps.IdempotencyScopeValue,
    ): IdempotencyRow {
        val rows = em.createQuery(
            """
                select i.status, i.responseSnapshot
                  from IdempotencyKeyEntity i
                 where i.memberId = :memberId
                   and i.scope = :scope
                   and i.idempotencyKey = :idempotencyKey
            """.trimIndent(),
            Array<Any>::class.java,
        )
            .setParameter("memberId", memberId)
            .setParameter("scope", scope)
            .setParameter("idempotencyKey", idempotencyKey)
            .resultList
        if (rows.isEmpty()) {
            throw IllegalStateException("Idempotency key not found")
        }
        val row = rows.first()
        return IdempotencyRow(row[0].toString(), row[1] as String?)
    }

    fun loadOutboxEvents(transferId: Long): List<OutboxRow> {
        val rows = em.createQuery(
            """
                select o.status, o.payload
                  from OutboxEventEntity o
                 where o.aggregateType = :aggregateType
                   and o.aggregateId = :aggregateId
            """.trimIndent(),
            Array<Any>::class.java,
        )
            .setParameter("aggregateType", "TRANSFER")
            .setParameter("aggregateId", transferId.toString())
            .resultList

        return rows.map { row -> OutboxRow(row[0].toString(), row[1] as String) }
    }

    fun loadOutboxEventIds(transferId: Long): List<Long> = em.createQuery(
        """
                select o.id
                  from OutboxEventEntity o
                 where o.aggregateType = :aggregateType
                   and o.aggregateId = :aggregateId
        """.trimIndent(),
        java.lang.Long::class.java,
    )
        .setParameter("aggregateType", "TRANSFER")
        .setParameter("aggregateId", transferId.toString())
        .resultList
        .map { it.toLong() }

    fun markOutboxSent(eventId: Long) {
        transactionTemplate.executeWithoutResult {
            em.createQuery(
                """
                    update OutboxEventEntity o
                       set o.status = org.yechan.remittance.transfer.OutboxEventProps.OutboxEventStatusValue.SENT
                     where o.id = :eventId
                """.trimIndent(),
            )
                .setParameter("eventId", eventId)
                .executeUpdate()
            em.flush()
        }
    }

    fun countOutboxEvents(): Long {
        val count = em.createQuery("select count(o) from OutboxEventEntity o", java.lang.Long::class.java)
            .singleResult
        return count?.toLong() ?: 0L
    }

    fun loadBalance(accountId: Long): BigDecimal = em.createQuery(
        "select a.balance from AccountEntity a where a.id = :accountId",
        BigDecimal::class.java,
    )
        .setParameter("accountId", accountId)
        .singleResult ?: throw IllegalStateException("Balance not found")

    fun loadLedgers(transferId: Long): List<LedgerRow> {
        val rows = em.createQuery(
            """
                select l.accountId, l.amount, l.side, l.createdAt
                  from LedgerEntity l
                 where l.transferId = :transferId
            """.trimIndent(),
            Array<Any>::class.java,
        )
            .setParameter("transferId", transferId)
            .resultList
        return rows.map { row ->
            LedgerRow(
                (row[0] as Number).toLong(),
                row[1] as BigDecimal,
                row[2].toString(),
                row[3] as LocalDateTime,
            )
        }
    }

    fun countLedgers(): Long {
        val count = em.createQuery("select count(l) from LedgerEntity l", java.lang.Long::class.java)
            .singleResult
        return count?.toLong() ?: 0L
    }

    fun countTransfers(): Long {
        val count = em.createQuery("select count(t) from TransferEntity t", java.lang.Long::class.java)
            .singleResult
        return count?.toLong() ?: 0L
    }

    fun markIdempotencyInProgress(
        memberId: Long,
        idempotencyKey: String,
        startedAt: LocalDateTime,
    ) {
        transactionTemplate.executeWithoutResult {
            em.createQuery(
                """
                    update IdempotencyKeyEntity i
                       set i.status = org.yechan.remittance.transfer.IdempotencyKeyProps.IdempotencyKeyStatusValue.IN_PROGRESS,
                           i.startedAt = :startedAt
                     where i.memberId = :memberId
                       and i.scope = org.yechan.remittance.transfer.IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER
                       and i.idempotencyKey = :idempotencyKey
                """.trimIndent(),
            )
                .setParameter("memberId", memberId)
                .setParameter("idempotencyKey", idempotencyKey)
                .setParameter("startedAt", startedAt)
                .executeUpdate()
            em.flush()
        }
    }

    fun markIdempotencyTimeoutBefore(
        cutoff: LocalDateTime,
        responseSnapshot: String,
        completedAt: LocalDateTime,
    ): Int = transactionTemplate.execute {
        val updated = em.createQuery(
            """
                    update IdempotencyKeyEntity i
                       set i.status = org.yechan.remittance.transfer.IdempotencyKeyProps.IdempotencyKeyStatusValue.TIMEOUT,
                           i.responseSnapshot = :responseSnapshot,
                           i.completedAt = :completedAt
                     where i.status = org.yechan.remittance.transfer.IdempotencyKeyProps.IdempotencyKeyStatusValue.IN_PROGRESS
                       and i.startedAt < :cutoff
            """.trimIndent(),
        )
            .setParameter("cutoff", cutoff)
            .setParameter("responseSnapshot", responseSnapshot)
            .setParameter("completedAt", completedAt)
            .executeUpdate()
        em.flush()
        updated
    } ?: 0

    fun setupAuthentication(): Result {
        val auth = registerAndIssueToken("tester")
        val authentication = tokenVerifier.verify(auth.accessToken)
        SecurityContextHolder.getContext().authentication = authentication
        return Result(auth, authentication)
    }

    data class AccountSeed(
        val accountId: Long,
        val bankCode: String,
        val accountNumber: String,
        val accountName: String,
        val balance: BigDecimal,
    )

    data class AuthSeed(
        val accessToken: String,
        val email: String,
        val password: String,
    )

    data class LedgerRow(
        val accountId: Long,
        val amount: BigDecimal,
        val side: String,
        val createdAt: LocalDateTime,
    )

    data class OutboxRow(
        val status: String,
        val payload: String,
    )

    data class IdempotencyRow(
        val status: String,
        val responseSnapshot: String?,
    )

    data class Result(
        val auth: AuthSeed,
        val authentication: Authentication,
    )
}
