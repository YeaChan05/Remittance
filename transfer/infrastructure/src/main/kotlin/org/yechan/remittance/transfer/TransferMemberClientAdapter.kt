package org.yechan.remittance.transfer

import org.yechan.remittance.member.internal.contract.MemberExistenceInternalApi
import org.yechan.remittance.member.internal.contract.MemberExistsRequest

class TransferMemberClientAdapter(
    private val memberExistenceInternalApi: MemberExistenceInternalApi,
) : TransferMemberClient {
    override fun exists(memberId: Long): Boolean = memberExistenceInternalApi.exists(MemberExistsRequest(memberId)).exists
}
