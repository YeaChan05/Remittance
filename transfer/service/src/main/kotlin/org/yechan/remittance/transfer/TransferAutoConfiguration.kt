package org.yechan.remittance.transfer

import java.time.Clock
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.yechan.remittance.account.AccountRepository
import org.yechan.remittance.member.MemberRepository

@AutoConfiguration
class TransferAutoConfiguration {
    @Bean
    fun transferIdempotencyHandler(
        idempotencyKeyRepository: IdempotencyKeyRepository,
        transferSnapshotUtil: TransferSnapshotUtil
    ): TransferIdempotencyHandler {
        return TransferIdempotencyHandler(idempotencyKeyRepository, transferSnapshotUtil)
    }

    @Bean
    fun transferProcessService(
        accountRepository: AccountRepository,
        transferRepository: TransferRepository,
        outboxEventRepository: OutboxEventRepository,
        idempotencyKeyRepository: IdempotencyKeyRepository,
        dailyLimitUsageRepository: DailyLimitUsageRepository,
        memberRepository: MemberRepository,
        transferSnapshotUtil: TransferSnapshotUtil
    ): TransferProcessService {
        return TransferProcessService(
            accountRepository,
            transferRepository,
            outboxEventRepository,
            idempotencyKeyRepository,
            dailyLimitUsageRepository,
            memberRepository,
            transferSnapshotUtil
        )
    }

    @Bean
    fun ledgerWriter(ledgerRepository: LedgerRepository): LedgerWriter {
        return LedgerWriter(ledgerRepository)
    }

    @Bean
    fun transferQueryUseCase(
        accountRepository: AccountRepository,
        transferRepository: TransferRepository
    ): TransferQueryUseCase {
        return TransferQueryService(accountRepository, transferRepository)
    }

    @Bean
    fun transferCreateUseCase(
        idempotencyHandler: TransferIdempotencyHandler,
        transferProcessService: TransferProcessService,
        ledgerWriter: LedgerWriter,
        transferSnapshotUtil: TransferSnapshotUtil,
        clock: Clock
    ): TransferCreateUseCase {
        return TransferService(
            idempotencyHandler,
            transferProcessService,
            ledgerWriter,
            transferSnapshotUtil,
            clock
        )
    }

    @Bean
    @ConditionalOnBean(TransferEventPublisher::class)
    fun transferEventPublishUseCase(
        outboxEventRepository: OutboxEventRepository,
        transferEventPublisher: TransferEventPublisher
    ): TransferEventPublishUseCase {
        return TransferEventPublishService(outboxEventRepository, transferEventPublisher)
    }
}
