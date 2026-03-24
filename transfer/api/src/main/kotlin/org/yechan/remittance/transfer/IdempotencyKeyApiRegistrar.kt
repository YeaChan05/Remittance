package org.yechan.remittance.transfer

import org.springframework.context.annotation.Import

@Import(IdempotencyKeyController::class)
class IdempotencyKeyApiRegistrar
