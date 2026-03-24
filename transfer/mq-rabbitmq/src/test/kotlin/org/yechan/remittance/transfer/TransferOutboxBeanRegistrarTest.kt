package org.yechan.remittance.transfer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.env.MapPropertySource

class TransferOutboxBeanRegistrarTest {
    @Test
    fun `enabled 기본값에서는 publisher 관련 빈을 등록한다`() {
        val context = createContext()

        assertThat(context.getBean(TransferEventPublisher::class.java)).isInstanceOf(TransferEventPublisherImpl::class.java)
        assertThat(context.getBean(TransferOutboxPublisher::class.java)).isNotNull

        context.close()
    }

    @Test
    fun `outbox enabled false이면 publisher 빈을 등록하지 않는다`() {
        val context = createContext("transfer.outbox.enabled" to "false")

        assertThat(context.getBeansOfType(TransferEventPublisher::class.java)).isEmpty()
        assertThat(context.getBeansOfType(TransferOutboxPublisher::class.java)).isEmpty()

        context.close()
    }

    @Test
    fun `publisher enabled false이면 event publisher만 등록한다`() {
        val context = createContext("transfer.outbox.publisher.enabled" to "false")

        assertThat(context.getBean(TransferEventPublisher::class.java)).isNotNull
        assertThat(context.getBeansOfType(TransferOutboxPublisher::class.java)).isEmpty()

        context.close()
    }

    private fun createContext(vararg properties: Pair<String, String>): AnnotationConfigApplicationContext = AnnotationConfigApplicationContext().apply {
        environment.propertySources.addFirst(MapPropertySource("test", mapOf(*properties)))
        beanFactory.registerSingleton("rabbitTemplate", mock(RabbitTemplate::class.java))
        beanFactory.registerSingleton(
            "transferOutboxProperties",
            TransferOutboxProperties(),
        )
        beanFactory.registerSingleton(
            "transferEventPublisherProperties",
            TransferEventPublisherProperties(),
        )
        beanFactory.registerSingleton(
            "transferEventPublishUseCase",
            mock(TransferEventPublishUseCase::class.java),
        )
        register(TestConfiguration::class.java)
        refresh()
    }

    @Configuration(proxyBeanMethods = false)
    @Import(TransferOutboxBeanRegistrar::class)
    class TestConfiguration
}
