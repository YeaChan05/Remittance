package org.yechan.remittance.transfer

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.yechan.remittance.transfer.config.TransferApplicationSecurityBeanRegistrar
import java.time.Clock

@Import(
    TransferApplicationBeanRegistrar::class,
    TransferApplicationSecurityBeanRegistrar::class,
)
@SpringBootApplication(
    excludeName = [
        "org.yechan.remittance.account.AccountBeanRegistrar",
        "org.yechan.remittance.account.internal.adapter.AccountInternalApiBeanRegistrar",
        "org.yechan.remittance.member.MemberBeanRegistrar",
        "org.yechan.remittance.member.internal.adapter.MemberInternalApiBeanRegistrar",
    ],
)
class TransferApiApplication

class TransferApplicationBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<Clock> {
            Clock.systemUTC()
        }
    })

fun main(args: Array<String>) {
    runApplication<TransferApiApplication>(*args)
}
