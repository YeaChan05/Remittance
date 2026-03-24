package org.yechan.remittance.transfer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.env.MapPropertySource
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class IdempotencyKeyBeanRegistrarTest {
    @Test
    fun `자동 설정은 idempotency key use case 빈을 등록한다`() {
        val context = AnnotationConfigApplicationContext().apply {
            environment.propertySources.addFirst(
                MapPropertySource(
                    "test",
                    mapOf("transfer.idempotency-key.expires-in" to "PT5M"),
                ),
            )
            beanFactory.registerSingleton("idempotencyKeyRepository", mock(IdempotencyKeyRepository::class.java))
            beanFactory.registerSingleton(
                "clock",
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
            )
            register(TestConfiguration::class.java)
            refresh()
        }

        assertThat(context.getBean(IdempotencyKeyCreateUseCase::class.java)).isInstanceOf(IdempotencyKeyService::class.java)

        context.close()
    }

    @Configuration(proxyBeanMethods = false)
    @Import(IdempotencyKeyAutoConfiguration::class, IdempotencyKeyBeanRegistrar::class)
    class TestConfiguration
}
