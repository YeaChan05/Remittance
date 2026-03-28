package org.yechan.remittance.account

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.yechan.remittance.account.config.AccountApplicationSecurityBeanRegistrar
import java.time.Clock

@Import(AccountApplicationBeanRegistrar::class, AccountApplicationSecurityBeanRegistrar::class)
@SpringBootApplication
class AccountApiApplication

class AccountApplicationBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<Clock> {
            Clock.systemUTC()
        }
    })

fun main(args: Array<String>) {
    runApplication<AccountApiApplication>(*args)
}
