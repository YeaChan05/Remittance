package org.yechan.remittance.transfer.repository

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.yechan.remittance.transfer.DailyLimitUsageRepository
import org.yechan.remittance.transfer.IdempotencyKeyRepository
import org.yechan.remittance.transfer.LedgerRepository
import org.yechan.remittance.transfer.OutboxEventRepository
import org.yechan.remittance.transfer.TransferRepository

@AutoConfiguration(before = [DataJpaRepositoriesAutoConfiguration::class])
@EntityScan(
    basePackageClasses = [
        IdempotencyKeyEntity::class,
        TransferEntity::class,
        OutboxEventEntity::class,
        LedgerEntity::class,
        DailyLimitUsageEntity::class
    ]
)
@EnableJpaRepositories(
    basePackageClasses = [
        IdempotencyKeyJpaRepository::class,
        TransferJpaRepository::class,
        OutboxEventJpaRepository::class,
        LedgerJpaRepository::class,
        DailyLimitUsageJpaRepository::class
    ]
)
class TransferRepositoryAutoConfiguration {
    @Bean
    fun idempotencyKeyRepository(repository: IdempotencyKeyJpaRepository): IdempotencyKeyRepository {
        return IdempotencyKeyRepositoryImpl(repository)
    }

    @Bean
    fun transferRepository(repository: TransferJpaRepository): TransferRepository {
        return TransferRepositoryImpl(repository)
    }

    @Bean
    fun outboxEventRepository(repository: OutboxEventJpaRepository): OutboxEventRepository {
        return OutboxEventRepositoryImpl(repository)
    }

    @Bean
    fun ledgerRepository(repository: LedgerJpaRepository): LedgerRepository {
        return LedgerRepositoryImpl(repository)
    }

    @Bean
    fun dailyLimitUsageRepository(repository: DailyLimitUsageJpaRepository): DailyLimitUsageRepository {
        return DailyLimitUsageRepositoryImpl(repository)
    }
}
