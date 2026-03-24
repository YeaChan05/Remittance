package org.yechan.remittance.member

import org.springframework.context.annotation.Import

@Import(MemberController::class, AuthController::class)
class MemberApiRegistrar
