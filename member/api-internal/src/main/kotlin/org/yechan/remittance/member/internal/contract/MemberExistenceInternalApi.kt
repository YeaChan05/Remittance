package org.yechan.remittance.member.internal.contract

fun interface MemberExistenceInternalApi {
    fun exists(request: MemberExistsRequest): MemberExistsResponse
}
