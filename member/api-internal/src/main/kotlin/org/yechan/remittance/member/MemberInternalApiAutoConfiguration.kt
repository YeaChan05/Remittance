package org.yechan.remittance.member

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import

@Import(MemberInternalApiBeanRegistrar::class)
@AutoConfiguration
class MemberInternalApiAutoConfiguration

class MemberInternalApiBeanRegistrar : BeanRegistrarDsl({
    registerBean<MemberInternalApi> {
        MemberInternalAdapter(bean())
    }
})
