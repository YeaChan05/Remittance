package org.yechan.remittance

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Import(GlobalExceptionHandler::class, CommonApiBeanRegistrar::class)
@AutoConfiguration
class CommonApiAutoConfiguration

class CommonApiBeanRegistrar : BeanRegistrarDsl({
    registerBean<LoginUserIdArgumentResolver> {
        LoginUserIdArgumentResolver()
    }

    registerBean<WebMvcConfigurer> {
        val loginUserIdArgumentResolver = bean<LoginUserIdArgumentResolver>()

        object : WebMvcConfigurer {
            override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
                resolvers.add(loginUserIdArgumentResolver)
            }
        }
    }
})
