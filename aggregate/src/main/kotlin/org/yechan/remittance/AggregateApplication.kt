package org.yechan.remittance

import java.time.Clock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.yechan.remittance.config.AggregateSecurityConfiguration

@SpringBootApplication
@Import(AggregateSecurityConfiguration::class)
class AggregateApplication {
    @Bean
    fun clock(): Clock {
        return Clock.systemUTC()
    }
}

fun main(args: Array<String>) {
    runApplication<AggregateApplication>(*args)
}
