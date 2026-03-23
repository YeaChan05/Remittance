package org.yechan.remittance.transfer

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import
import java.time.Clock

@Import(TransferBeanRegistrar::class)
@AutoConfiguration
class TransferAutoConfiguration

class TransferBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<TransferIdempotencyHandler> {
            TransferIdempotencyHandler(
                bean(),
                bean(),
            )
        }

        registerBean<TransferProcessService> {
            TransferProcessService(
                bean(),
                bean(),
                bean(),
                bean(),
                bean(),
                bean(),
                bean(),
            )
        }

        registerBean<LedgerWriter> {
            LedgerWriter(bean())
        }

        registerBean<TransferQueryUseCase> {
            TransferQueryService(
                bean(),
                bean(),
            )
        }

        registerBean<TransferCreateUseCase> {
            TransferService(
                bean(),
                bean(),
                bean(),
                bean(),
                bean<Clock>(),
            )
        }

        registerBean<TransferEventPublishUseCase> {
            TransferEventPublishService(
                bean(),
                bean(),
                bean(),
            )
        }

        registerBean<OutboxEventStatusUpdater> {
            OutboxEventStatusUpdater(bean())
        }
    })
