package org.yechan.remittance.transfer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicReference

class IdempotencyKeyServiceTest {
    @Test
    fun `멱등키 생성은 만료 시간을 포함해 저장한다`() {
        val saved = AtomicReference<IdempotencyKeyProps>()
        val repository = TestIdempotencyKeyRepository(saved)
        val now = LocalDateTime.parse("2026-01-01T00:00:00")
        val clock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        val properties = IdempotencyKeyProperties(Duration.ofHours(1))
        val service = IdempotencyKeyService(repository, clock, properties.expiresIn)

        val created = service.create(
            object : IdempotencyKeyCreateProps {
                override val memberId: Long = 10L
                override val scope: IdempotencyKeyProps.IdempotencyScopeValue =
                    IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER
            },
        )

        assertThat(created.memberId).isEqualTo(10L)
        assertThat(created.idempotencyKey).isNotBlank()
        assertThat(created.expiresAt).isEqualTo(now.plus(Duration.ofHours(1)))
        assertThat(created.scope).isEqualTo(IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER)
        assertThat(created.status).isEqualTo(IdempotencyKeyProps.IdempotencyKeyStatusValue.BEFORE_START)
        assertThat(created.requestHash).isNull()
        assertThat(created.responseSnapshot).isNull()
        assertThat(created.startedAt).isNull()
        assertThat(created.completedAt).isNull()
        assertThat(saved.get()).isNotNull()
        assertThat(saved.get().idempotencyKey).isEqualTo(created.idempotencyKey)
    }

    private class TestIdempotencyKeyRepository(
        private val saved: AtomicReference<IdempotencyKeyProps>,
    ) : IdempotencyKeyRepository {
        override fun save(props: IdempotencyKeyProps): IdempotencyKeyModel {
            saved.set(props)
            return IdempotencyKey(
                saved.get().memberId,
                props.memberId,
                props.idempotencyKey,
                requireNotNull(props.expiresAt),
                props.scope,
                requireNotNull(props.status),
                props.requestHash,
                props.responseSnapshot,
                props.startedAt,
                props.completedAt,
            )
        }

        override fun findByKey(
            memberId: Long,
            scope: IdempotencyKeyProps.IdempotencyScopeValue,
            idempotencyKey: String,
        ): IdempotencyKeyModel? = null

        override fun tryMarkInProgress(
            memberId: Long,
            scope: IdempotencyKeyProps.IdempotencyScopeValue,
            idempotencyKey: String,
            requestHash: String,
            startedAt: LocalDateTime,
        ): Boolean = false

        override fun markSucceeded(
            memberId: Long,
            scope: IdempotencyKeyProps.IdempotencyScopeValue,
            idempotencyKey: String,
            responseSnapshot: String,
            completedAt: LocalDateTime,
        ): IdempotencyKeyModel = throw UnsupportedOperationException()

        override fun markFailed(
            memberId: Long,
            scope: IdempotencyKeyProps.IdempotencyScopeValue,
            idempotencyKey: String,
            responseSnapshot: String,
            completedAt: LocalDateTime,
        ): IdempotencyKeyModel = throw UnsupportedOperationException()

        override fun markTimeoutBefore(
            cutoff: LocalDateTime,
            responseSnapshot: String,
        ): Int = 0
    }
}
