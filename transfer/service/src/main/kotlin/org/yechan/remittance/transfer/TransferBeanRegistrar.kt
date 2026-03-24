package org.yechan.remittance.transfer

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import java.time.Clock

@AutoConfiguration
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
