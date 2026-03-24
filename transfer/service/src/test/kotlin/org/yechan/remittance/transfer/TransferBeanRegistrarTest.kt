package org.yechan.remittance.transfer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TransferBeanRegistrarTest {
    @Test
    fun `자동 설정은 transfer 관련 빈을 등록한다`() {
        val context = AnnotationConfigApplicationContext().apply {
            beanFactory.registerSingleton("transferAccountClient", mock(TransferAccountClient::class.java))
            beanFactory.registerSingleton("transferRepository", mock(TransferRepository::class.java))
            beanFactory.registerSingleton("outboxEventRepository", mock(OutboxEventRepository::class.java))
            beanFactory.registerSingleton("idempotencyKeyRepository", mock(IdempotencyKeyRepository::class.java))
            beanFactory.registerSingleton("dailyLimitUsageRepository", mock(DailyLimitUsageRepository::class.java))
            beanFactory.registerSingleton("transferMemberClient", mock(TransferMemberClient::class.java))
            beanFactory.registerSingleton("transferSnapshotUtil", mock(TransferSnapshotUtil::class.java))
            beanFactory.registerSingleton("ledgerRepository", mock(LedgerRepository::class.java))
            beanFactory.registerSingleton("transferEventPublisher", mock(TransferEventPublisher::class.java))
            beanFactory.registerSingleton(
                "clock",
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
            )
            register(TestConfiguration::class.java)
            refresh()
        }

        assertThat(context.getBean(TransferIdempotencyHandler::class.java)).isNotNull
        assertThat(context.getBean(TransferProcessService::class.java)).isNotNull
        assertThat(context.getBean(LedgerWriter::class.java)).isNotNull
        assertThat(context.getBean(TransferQueryUseCase::class.java)).isInstanceOf(TransferQueryService::class.java)
        assertThat(context.getBean(TransferCreateUseCase::class.java)).isInstanceOf(TransferService::class.java)
        assertThat(context.getBean(TransferEventPublishUseCase::class.java)).isInstanceOf(TransferEventPublishService::class.java)
        assertThat(context.getBean(OutboxEventStatusUpdater::class.java)).isNotNull

        context.close()
    }

    @Configuration(proxyBeanMethods = false)
    @Import(TransferBeanRegistrar::class)
    class TestConfiguration
}
