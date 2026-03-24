package org.yechan.remittance.transfer.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.yechan.remittance.BaseEntity
import org.yechan.remittance.transfer.IdempotencyKeyModel
import org.yechan.remittance.transfer.IdempotencyKeyProps
import java.time.LocalDateTime

@Entity
@Table(
    name = "idempotency_key",
    catalog = "integration",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_idempotency_key_client_scope",
            columnNames = ["client_id", "scope", "idempotency_key"],
        ),
    ],
)
class IdempotencyKeyEntity() :
    BaseEntity(),
    IdempotencyKeyModel {
    @field:Column(name = "client_id", nullable = false)
    final override var memberId: Long = 0
        private set

    @field:Column(name = "idempotency_key", nullable = false)
    final override var idempotencyKey: String = ""
        private set

    @field:Column(nullable = false)
    final override var expiresAt: LocalDateTime = LocalDateTime.MIN
        private set

    @field:Enumerated(EnumType.STRING)
    @field:Column(name = "scope", nullable = false)
    final override var scope: IdempotencyKeyProps.IdempotencyScopeValue =
        IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER
        private set

    @field:Enumerated(EnumType.STRING)
    @field:Column
    final override var status: IdempotencyKeyProps.IdempotencyKeyStatusValue =
        IdempotencyKeyProps.IdempotencyKeyStatusValue.BEFORE_START
        private set

    @field:Column
    final override var requestHash: String? = null
        private set

    @field:Column
    final override var responseSnapshot: String? = null
        private set

    @field:Column
    final override var startedAt: LocalDateTime? = null
        private set

    @field:Column
    final override var completedAt: LocalDateTime? = null
        private set

    private constructor(
        memberId: Long,
        idempotencyKey: String,
        expiresAt: LocalDateTime,
        scope: IdempotencyKeyProps.IdempotencyScopeValue,
    ) : this() {
        this.memberId = memberId
        this.idempotencyKey = idempotencyKey
        this.expiresAt = expiresAt
        this.scope = scope
    }

    override val idempotencyKeyId: Long?
        get() = id

    override fun tryMarkInProgress(requestHash: String, startedAt: LocalDateTime?): Boolean {
        if (status != IdempotencyKeyProps.IdempotencyKeyStatusValue.BEFORE_START) {
            return false
        }
        status = IdempotencyKeyProps.IdempotencyKeyStatusValue.IN_PROGRESS
        this.requestHash = requestHash
        this.startedAt = startedAt
        return true
    }

    override fun markSucceeded(responseSnapshot: String, completedAt: LocalDateTime) {
        status = IdempotencyKeyProps.IdempotencyKeyStatusValue.SUCCEEDED
        this.responseSnapshot = responseSnapshot
        this.completedAt = completedAt
    }

    override fun markFailed(responseSnapshot: String, completedAt: LocalDateTime) {
        status = IdempotencyKeyProps.IdempotencyKeyStatusValue.FAILED
        this.responseSnapshot = responseSnapshot
        this.completedAt = completedAt
    }

    override fun markTimeoutIfBefore(
        cutoff: LocalDateTime,
        responseSnapshot: String,
        completedAt: LocalDateTime,
    ): Boolean {
        if (status != IdempotencyKeyProps.IdempotencyKeyStatusValue.IN_PROGRESS) {
            return false
        }
        if (startedAt == null || !startedAt!!.isBefore(cutoff)) {
            return false
        }
        status = IdempotencyKeyProps.IdempotencyKeyStatusValue.TIMEOUT
        this.responseSnapshot = responseSnapshot
        this.completedAt = completedAt
        return true
    }

    companion object {
        fun create(props: IdempotencyKeyProps): IdempotencyKeyEntity = IdempotencyKeyEntity(
            props.memberId,
            props.idempotencyKey,
            requireNotNull(props.expiresAt),
            props.scope,
        )
    }
}
