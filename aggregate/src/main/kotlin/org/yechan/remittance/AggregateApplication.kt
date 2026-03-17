package org.yechan.remittance

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.yechan.remittance.config.AggregateSecurityConfiguration
import java.time.Clock

@Import(AggregateApplicationBeanRegistrar::class, AggregateSecurityConfiguration::class)
@SpringBootApplication
class AggregateApplication

class AggregateApplicationBeanRegistrar : BeanRegistrarDsl({
    registerBean<Clock> {
        Clock.systemUTC()
    }
})

fun main(args: Array<String>) {
    runApplication<AggregateApplication>(*args)
}
