package org.yechan.remittance.transfer.repository

import jakarta.persistence.EntityManager
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional
import org.yechan.remittance.transfer.DailyLimitUsageModel
import org.yechan.remittance.transfer.DailyLimitUsageProps
import org.yechan.remittance.transfer.DailyLimitUsageRepository
import org.yechan.remittance.transfer.TransferAccountIdentifier
import org.yechan.remittance.transfer.TransferProps
import java.math.BigDecimal
import java.time.LocalDate

open class DailyLimitUsageRepositoryImpl(
    private val repository: DailyLimitUsageJpaRepository,
    private val entityManager: EntityManager,
) : DailyLimitUsageRepository {
    @Transactional
    override fun findOrCreateForUpdate(
        identifier: TransferAccountIdentifier,
        scope: TransferProps.TransferScopeValue,
        usageDate: LocalDate,
    ): DailyLimitUsageModel = repository.findForUpdate(requireNotNull(identifier.accountId), scope, usageDate)
        ?: createAndLock(identifier, scope, usageDate)

    private fun createAndLock(
        identifier: TransferAccountIdentifier,
        scope: TransferProps.TransferScopeValue,
        usageDate: LocalDate,
    ): DailyLimitUsageModel {
        try {
            repository.saveAndFlush(
                DailyLimitUsageEntity.create(
                    DailyLimitUsageCreateCommand(
                        requireNotNull(identifier.accountId),
                        scope,
                        usageDate,
                        BigDecimal.ZERO,
                    ),
                ),
            )
        } catch (_: DataIntegrityViolationException) {
            entityManager.clear()
        }

        return repository.findForUpdate(requireNotNull(identifier.accountId), scope, usageDate)
            ?: throw IllegalStateException("Daily limit usage not found")
    }

    private data class DailyLimitUsageCreateCommand(
        override val accountId: Long,
        override val scope: TransferProps.TransferScopeValue,
        override val usageDate: LocalDate,
        override val usedAmount: BigDecimal,
    ) : DailyLimitUsageProps
}
