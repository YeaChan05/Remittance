package org.yechan.remittance.transfer.repository

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.yechan.remittance.transfer.DailyLimitUsageRepository
import org.yechan.remittance.transfer.IdempotencyKeyRepository
import org.yechan.remittance.transfer.LedgerRepository
import org.yechan.remittance.transfer.OutboxEventRepository
import org.yechan.remittance.transfer.TransferRepository

@Import(TransferRepositoryBeanRegistrar::class)
@AutoConfiguration(before = [DataJpaRepositoriesAutoConfiguration::class])
@EntityScan(
    basePackageClasses = [
        IdempotencyKeyEntity::class,
        TransferEntity::class,
        OutboxEventEntity::class,
        LedgerEntity::class,
        DailyLimitUsageEntity::class,
    ],
)
@EnableJpaRepositories(
    basePackageClasses = [
        IdempotencyKeyJpaRepository::class,
        TransferJpaRepository::class,
        OutboxEventJpaRepository::class,
        LedgerJpaRepository::class,
        DailyLimitUsageJpaRepository::class,
    ],
)
class TransferRepositoryAutoConfiguration

class TransferRepositoryBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<IdempotencyKeyRepository> {
            IdempotencyKeyRepositoryImpl(bean())
        }

        registerBean<TransferRepository> {
            TransferRepositoryImpl(bean())
        }

        registerBean<OutboxEventRepository> {
            OutboxEventRepositoryImpl(bean())
        }

        registerBean<LedgerRepository> {
            LedgerRepositoryImpl(bean())
        }

        registerBean<DailyLimitUsageRepository> {
            DailyLimitUsageRepositoryImpl(bean())
        }
    })
