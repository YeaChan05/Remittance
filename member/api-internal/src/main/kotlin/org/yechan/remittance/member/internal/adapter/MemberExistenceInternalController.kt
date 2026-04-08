package org.yechan.remittance.member.internal.adapter

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.yechan.remittance.member.MemberExistenceQueryUseCase
import org.yechan.remittance.member.internal.contract.MemberExistsRequest
import org.yechan.remittance.member.internal.contract.MemberExistsResponse

@RestController
@RequestMapping("/internal/members")
class MemberExistenceInternalController(
    private val memberExistenceQueryUseCase: MemberExistenceQueryUseCase,
) {
    @PostMapping("/existence")
    fun exists(
        @RequestBody request: MemberExistsRequest,
    ): MemberExistsResponse = MemberExistsResponse(memberExistenceQueryUseCase.exists(request.memberId))
}
