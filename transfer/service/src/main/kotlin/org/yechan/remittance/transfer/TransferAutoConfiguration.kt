package org.yechan.remittance.transfer

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import
import org.yechan.remittance.account.AccountRepository
import org.yechan.remittance.member.MemberRepository
import java.time.Clock

@Import(TransferBeanRegistrar::class)
@AutoConfiguration
class TransferAutoConfiguration

class TransferBeanRegistrar : BeanRegistrarDsl({
    registerBean<TransferIdempotencyHandler> {
        TransferIdempotencyHandler(
            bean(),
            bean()
        )
    }

    registerBean<TransferProcessService> {
        TransferProcessService(
            bean<AccountRepository>(),
            bean<TransferRepository>(),
            bean(),
            bean(),
            bean(),
            bean<MemberRepository>(),
            bean()
        )
    }

    registerBean<LedgerWriter> {
        LedgerWriter(bean())
    }

    registerBean<TransferQueryUseCase> {
        TransferQueryService(
            bean<AccountRepository>(),
            bean<TransferRepository>()
        )
    }

    registerBean<TransferCreateUseCase> {
        TransferService(
            bean(),
            bean(),
            bean(),
            bean(),
            bean<Clock>()
        )
    }

    registerBean<TransferEventPublishUseCase> {
        TransferEventPublishService(
            bean(),
            bean()
        )
    }
})
