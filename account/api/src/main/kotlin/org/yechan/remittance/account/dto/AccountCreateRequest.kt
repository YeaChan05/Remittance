package org.yechan.remittance.account.dto

import jakarta.validation.constraints.NotBlank

data class AccountCreateRequest(
    @field:NotBlank(message = "Invalid bank code")
    val bankCode: String,
    @field:NotBlank(message = "Invalid account number")
    val accountNumber: String,
    @field:NotBlank(message = "Invalid account name")
    val accountName: String
)
