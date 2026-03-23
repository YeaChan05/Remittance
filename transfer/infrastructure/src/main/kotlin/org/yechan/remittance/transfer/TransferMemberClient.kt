package org.yechan.remittance.transfer

fun interface TransferMemberClient {
    fun exists(memberId: Long): Boolean
}
