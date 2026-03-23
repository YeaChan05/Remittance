package org.yechan.remittance.auth

import jakarta.validation.constraints.Pattern

data class AuthLoginRequest(
    @field:Pattern(
        regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
        message = "Invalid email address",
    )
    override val email: String,
    @field:Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*])[a-zA-Z\\d!@#$%^&*]{8,}$",
        message = "Invalid password format. Password must contain at least one letter, one number, and one special character.",
    )
    override val password: String,
) : AuthLoginProps
