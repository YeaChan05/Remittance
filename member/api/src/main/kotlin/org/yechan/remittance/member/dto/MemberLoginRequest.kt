package org.yechan.remittance.member.dto

import jakarta.validation.constraints.Pattern
import org.yechan.remittance.member.MemberLoginProps

data class MemberLoginRequest(
    @field:Pattern(
        regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
        message = "Invalid email address"
    )
    override val email: String,
    @field:Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*])[a-zA-Z\\d!@#$%^&*]{8,}$",
        message = "Invalid password format. Password must contain at least one letter, one number, and one special character."
    )
    override val password: String
) : MemberLoginProps
