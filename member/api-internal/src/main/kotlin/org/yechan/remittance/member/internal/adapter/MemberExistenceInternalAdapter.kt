package org.yechan.remittance.member.internal.adapter

import org.yechan.remittance.member.MemberExistenceQueryUseCase
import org.yechan.remittance.member.internal.contract.MemberExistenceInternalApi
import org.yechan.remittance.member.internal.contract.MemberExistsRequest
import org.yechan.remittance.member.internal.contract.MemberExistsResponse

class MemberExistenceInternalAdapter(
    private val memberExistenceQueryUseCase: MemberExistenceQueryUseCase,
) : MemberExistenceInternalApi {
    override fun exists(request: MemberExistsRequest): MemberExistsResponse = MemberExistsResponse(memberExistenceQueryUseCase.exists(request.memberId))
}
