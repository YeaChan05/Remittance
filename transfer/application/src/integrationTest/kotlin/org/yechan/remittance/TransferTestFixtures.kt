package org.yechan.remittance

import jakarta.persistence.EntityManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.support.TransactionTemplate
import org.yechan.remittance.transfer.IdempotencyKeyProps
import org.yechan.remittance.transfer.config.TransferApplicationAccountStore
import org.yechan.remittance.transfer.config.TransferApplicationMemberStore
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

class TransferTestFixtures(
    private val accountStore: TransferApplicationAccountStore,
    private val memberStore: TransferApplicationMemberStore,
    private val em: EntityManager,
    private val transactionTemplate: TransactionTemplate,
    private val tokenGenerator: TokenGenerator,
    private val tokenVerifier: TokenVerifier,
) {
    private val nextMemberId = AtomicLong(1)

    fun reset() {
        SecurityContextHolder.clearContext()
        accountStore.clear()
        memberStore.clear()
        nextMemberId.set(1)
        transactionTemplate.executeWithoutResult {
            em.createQuery("delete from DailyLimitUsageEntity").executeUpdate()
            em.createQuery("delete from LedgerEntity").executeUpdate()
            em.createQuery("delete from OutboxEventEntity").executeUpdate()
            em.createQuery("delete from TransferEntity").executeUpdate()
            em.createQuery("delete from IdempotencyKeyEntity").executeUpdate()
            em.flush()
            em.clear()
        }
    }

    fun createAccountWithBalance(
        memberId: Long,
        accountName: String,
        balance: BigDecimal,
    ): AccountSeed {
        memberStore.register(memberId)
        val account = accountStore.create(memberId, balance)
        return AccountSeed(
            account.accountId,
            "001",
            "ACC-${account.accountId}",
            accountName,
            balance,
        )
    }

    fun registerAndIssueToken(name: String): AuthSeed {
        val memberId = nextMemberId.getAndIncrement()
        memberStore.register(memberId)
        val accessToken = tokenGenerator.generate(memberId).accessToken
        return AuthSeed(accessToken, "$name-$memberId@test.local", "unused")
    }

    fun setupAuthentication(): Result {
        val auth = registerAndIssueToken("tester")
        val authentication = tokenVerifier.verify(auth.accessToken)
        SecurityContextHolder.getContext().authentication = authentication
        return Result(auth, authentication)
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
        Long::class.java,
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

    fun countOutboxEvents(): Long = em.createQuery("select count(o) from OutboxEventEntity o", Long::class.java)
        .singleResult

    fun loadBalance(accountId: Long): BigDecimal = accountStore.find(accountId)?.balance
        ?: throw IllegalStateException("Balance not found")

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

    fun countLedgers(): Long = em.createQuery("select count(l) from LedgerEntity l", Long::class.java)
        .singleResult

    fun countTransfers(): Long = em.createQuery("select count(t) from TransferEntity t", Long::class.java)
        .singleResult

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
