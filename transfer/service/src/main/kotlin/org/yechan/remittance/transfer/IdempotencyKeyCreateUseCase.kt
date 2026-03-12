package org.yechan.remittance.transfer

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

fun interface IdempotencyKeyCreateUseCase {
    fun create(props: IdempotencyKeyCreateProps): IdempotencyKeyModel
}

interface IdempotencyKeyCreateProps {
    val memberId: Long
    val scope: IdempotencyKeyProps.IdempotencyScopeValue
}

class IdempotencyKeyService(
    private val repository: IdempotencyKeyRepository,
    private val clock: Clock,
    private val expiresIn: Duration
) : IdempotencyKeyCreateUseCase {
    private val log = KotlinLogging.logger {}

    override fun create(props: IdempotencyKeyCreateProps): IdempotencyKeyModel {
        log.info { "idempotency.create.start memberId=${props.memberId} scope=${props.scope}" }
        val now = LocalDateTime.now(clock)
        val key = UUID.randomUUID().toString()
        log.info { "idempotency.create.persist memberId=${props.memberId} scope=${props.scope}" }
        return repository.save(GeneratedIdempotencyKeyProps(props, key, now))
    }

    private inner class GeneratedIdempotencyKeyProps(
        private val props: IdempotencyKeyCreateProps,
        private val key: String,
        private val now: LocalDateTime
    ) : IdempotencyKeyProps {
        override val memberId: Long
            get() = props.memberId

        override val idempotencyKey: String
            get() = key

        override val expiresAt: LocalDateTime
            get() = now.plus(expiresIn)

        override val scope: IdempotencyKeyProps.IdempotencyScopeValue
            get() = props.scope

        override val status: IdempotencyKeyProps.IdempotencyKeyStatusValue
            get() = IdempotencyKeyProps.IdempotencyKeyStatusValue.BEFORE_START

        override val requestHash: String? = null
        override val responseSnapshot: String? = null
        override val startedAt: LocalDateTime? = null
        override val completedAt: LocalDateTime? = null
    }
}
