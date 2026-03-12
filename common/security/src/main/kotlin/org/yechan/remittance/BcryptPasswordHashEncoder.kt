package org.yechan.remittance

import org.springframework.security.crypto.password.PasswordEncoder

class BcryptPasswordHashEncoder(
    private val passwordEncoder: PasswordEncoder
) : PasswordHashEncoder {
    override fun encode(password: String): String {
        return passwordEncoder.encode(password)!!
    }

    override fun matches(
        password: String,
        encodedPassword: String
    ): Boolean {
        return passwordEncoder.matches(password, encodedPassword)
    }
}
