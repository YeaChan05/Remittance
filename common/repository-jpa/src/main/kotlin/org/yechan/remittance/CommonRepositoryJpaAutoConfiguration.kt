package org.yechan.remittance

import com.p6spy.engine.spy.P6DataSource
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import javax.sql.DataSource

@AutoConfiguration
@ConditionalOnClass(P6DataSource::class, DataSource::class, P6SpySqlLogSuppressAspect::class)
@ConditionalOnProperty(prefix = "remittance.jpa.p6spy", name = ["enabled"], havingValue = "true")
class CommonRepositoryJpaAutoConfiguration {

    @Bean
    fun p6SpyDataSourceBeanPostProcessor(): BeanPostProcessor = P6SpyDataSourceBeanPostProcessor()

    @Bean
    fun p6SpySqlLogSuppressAspect(): P6SpySqlLogSuppressAspect = P6SpySqlLogSuppressAspect()
}

class P6SpyDataSourceBeanPostProcessor : BeanPostProcessor {

    override fun postProcessAfterInitialization(
        bean: Any,
        beanName: String,
    ): Any {
        if (bean !is DataSource || bean is P6DataSource) {
            return bean
        }
        return P6DataSource(bean)
    }
}
