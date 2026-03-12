package org.yechan.remittance.member

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import

@AutoConfiguration
@Import(MemberController::class, AuthController::class)
class MemberApiAutoConfiguration
