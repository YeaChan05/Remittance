package org.yechan.remittance.member

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.yechan.remittance.member.config.MemberApplicationSecurityBeanRegistrar
import java.time.Clock

@Import(MemberApplicationBeanRegistrar::class, MemberApplicationSecurityBeanRegistrar::class)
@SpringBootApplication
class MemberApiApplication

class MemberApplicationBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<Clock> {
            Clock.systemUTC()
        }
    })

fun main(args: Array<String>) {
    runApplication<MemberApiApplication>(*args)
}
