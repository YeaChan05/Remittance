package org.yechan.remittance.member

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.yechan.remittance.member.dto.MemberRegisterRequest
import org.yechan.remittance.member.dto.MemberRegisterResponse
import org.yechan.remittance.member.dto.toProps

@RestController
@RequestMapping("/members", version = "v1")
class MemberController(
    private val memberCreateUseCase: MemberCreateUseCase,
) : MemberApi {
    @PostMapping
    override fun register(
        @RequestBody @Valid request: MemberRegisterRequest,
    ): ResponseEntity<MemberRegisterResponse> {
        val model = memberCreateUseCase.register(request.toProps())
        return ResponseEntity.ok(MemberRegisterResponse(model.name))
    }
}
