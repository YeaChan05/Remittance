package org.yechan.remittance

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.time.Clock
import java.time.ZoneOffset

class AggregateApplicationBeanRegistrarTest {
    @Test
    fun `자동 설정은 utc clock 빈을 등록한다`() {
        val context = AnnotationConfigApplicationContext().apply {
            register(TestConfiguration::class.java)
            refresh()
        }

        val clock = context.getBean(Clock::class.java)

        assertThat(clock.zone).isEqualTo(ZoneOffset.UTC)

        context.close()
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AggregateApplicationBeanRegistrar::class)
    class TestConfiguration
}
