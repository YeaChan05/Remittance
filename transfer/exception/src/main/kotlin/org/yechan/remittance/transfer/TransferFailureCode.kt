package org.yechan.remittance.transfer

enum class TransferFailureCode {
    INSUFFICIENT_BALANCE,
    ACCOUNT_NOT_FOUND,
    INVALID_REQUEST,
    MEMBER_NOT_FOUND,
    OWNER_NOT_FOUND,
    DAILY_LIMIT_EXCEEDED
}
