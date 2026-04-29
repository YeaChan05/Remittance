package org.yechan.remittance

import com.p6spy.engine.spy.P6DataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.util.ReflectionTestUtils
import javax.sql.DataSource

class CommonRepositoryJpaAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(CommonRepositoryJpaAutoConfiguration::class.java))
        .withUserConfiguration(DataSourceTestConfiguration::class.java)

    @Test
    fun `p6spy 설정이 꺼져 있으면 DataSource를 감싸지 않는다`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(DataSource::class.java)
            assertThat(context.getBean(DataSource::class.java)).isNotInstanceOf(P6DataSource::class.java)
        }
    }

    @Test
    fun `p6spy 설정이 켜져 있으면 DataSource를 감싼다`() {
        contextRunner
            .withPropertyValues("remittance.jpa.p6spy.enabled=true")
            .run { context ->
                val dataSource = context.getBean(DataSource::class.java)

                assertThat(dataSource).isInstanceOf(P6DataSource::class.java)
                assertThat(ReflectionTestUtils.getField(dataSource, "realDataSource"))
                    .isInstanceOf(TestDataSource::class.java)
                assertThat(context).hasSingleBean(P6SpySqlLogSuppressAspect::class.java)
            }
    }

    @Configuration(proxyBeanMethods = false)
    private class DataSourceTestConfiguration {

        @Bean
        fun testDataSource(): TestDataSource = TestDataSource()
    }
}
