package org.yechan.remittance.account

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.env.MapPropertySource

class TransferNotificationInfrastructureBeanRegistrarTest {
    @Test
    fun `enabled 기본값에서는 notification infrastructure 빈을 등록한다`() {
        val context = createContext()

        assertThat(context.getBean(TransferNotificationPayloadParser::class.java)).isNotNull
        assertThat(context.getBean(DirectExchange::class.java).name).isEqualTo("transfer.exchange")
        assertThat(context.getBean(Queue::class.java).name).isEqualTo("transfer.completed.queue")
        assertThat(context.getBean(Binding::class.java).routingKey).isEqualTo("transfer.completed")
        assertThat(context.getBean(TransferNotificationConsumer::class.java)).isNotNull

        context.close()
    }

    @Test
    fun `enabled false이면 notification infrastructure 빈을 등록하지 않는다`() {
        val context = createContext("account.transfer-notification.enabled" to "false")

        assertThat(context.getBeansOfType(TransferNotificationPayloadParser::class.java)).isEmpty()
        assertThat(context.getBeansOfType(DirectExchange::class.java)).isEmpty()
        assertThat(context.getBeansOfType(Queue::class.java)).isEmpty()
        assertThat(context.getBeansOfType(Binding::class.java)).isEmpty()
        assertThat(context.getBeansOfType(TransferNotificationConsumer::class.java)).isEmpty()

        context.close()
    }

    private fun createContext(vararg properties: Pair<String, String>): AnnotationConfigApplicationContext = AnnotationConfigApplicationContext().apply {
        environment.propertySources.addFirst(MapPropertySource("test", mapOf(*properties)))
        beanFactory.registerSingleton(
            "transferNotificationConsumerProperties",
            TransferNotificationConsumerProperties(),
        )
        beanFactory.registerSingleton(
            "transferNotificationUseCase",
            mock(TransferNotificationUseCase::class.java),
        )
        register(TestConfiguration::class.java)
        refresh()
    }

    @Configuration(proxyBeanMethods = false)
    @Import(TransferNotificationInfrastructureBeanRegistrar::class)
    class TestConfiguration
}
