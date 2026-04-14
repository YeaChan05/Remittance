package org.yechan.remittance.member.dto

import jakarta.validation.constraints.Pattern
import org.yechan.remittance.member.MemberProps

data class MemberRegisterRequest(
    val name: String,
    @field:Pattern(
        regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
        message = "Invalid email address",
    )
    val email: String,
    @field:Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*])[a-zA-Z\\d!@#$%^&*]{8,}$",
        message = "Invalid password format. Password must contain at least one letter, one number, and one special character.",
    )
    val password: String,
)

fun MemberRegisterRequest.toProps(): MemberProps = object : MemberProps {
    override val name: String = this@toProps.name
    override val email: String = this@toProps.email
    override val password: String = this@toProps.password
}
