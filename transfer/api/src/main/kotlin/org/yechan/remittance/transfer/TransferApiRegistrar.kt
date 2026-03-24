package org.yechan.remittance.transfer

import org.springframework.context.annotation.Import

@Import(TransferController::class, WithdrawalController::class, DepositController::class)
class TransferApiRegistrar
