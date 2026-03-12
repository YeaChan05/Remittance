package org.yechan.remittance.account.repository

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.yechan.remittance.account.AccountRepository
import org.yechan.remittance.account.ProcessedEventRepository

@AutoConfiguration(before = [DataJpaRepositoriesAutoConfiguration::class])
@EntityScan(basePackageClasses = [AccountEntity::class, ProcessedEventEntity::class])
@EnableJpaRepositories(basePackageClasses = [AccountJpaRepository::class, ProcessedEventJpaRepository::class])
class AccountRepositoryAutoConfiguration {
    @Bean
    fun accountRepository(repository: AccountJpaRepository): AccountRepository {
        return AccountRepositoryImpl(repository)
    }

    @Bean
    fun processedEventRepository(repository: ProcessedEventJpaRepository): ProcessedEventRepository {
        return ProcessedEventRepositoryImpl(repository)
    }
}
