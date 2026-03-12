package org.yechan.remittance

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@AutoConfiguration
class PasswordEncoderAutoConfiguration {
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun passwordHashEncoder(passwordEncoder: PasswordEncoder): PasswordHashEncoder {
        return BcryptPasswordHashEncoder(passwordEncoder)
    }
}
