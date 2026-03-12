package org.yechan.remittance

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@AutoConfiguration
@Import(GlobalExceptionHandler::class)
class CommonApiAutoConfiguration : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(loginUserIdArgumentResolver())
    }

    @Bean
    fun loginUserIdArgumentResolver(): LoginUserIdArgumentResolver {
        return LoginUserIdArgumentResolver()
    }
}
