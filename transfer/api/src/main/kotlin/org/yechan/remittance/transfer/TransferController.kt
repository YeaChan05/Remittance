package org.yechan.remittance.transfer

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.yechan.remittance.LoginUserId
import org.yechan.remittance.transfer.dto.TransferQueryResponse
import org.yechan.remittance.transfer.dto.TransferRequest
import java.time.LocalDateTime

@RestController
@RequestMapping("/transfers")
class TransferController(
    private val transferCreateUseCase: TransferCreateUseCase,
    private val transferQueryUseCase: TransferQueryUseCase,
) : TransferApi {
    @PostMapping("/{idempotencyKey}")
    override fun transfer(
        @LoginUserId memberId: Long,
        @PathVariable idempotencyKey: String,
        @RequestBody props: TransferRequest,
    ): TransferResult = transferCreateUseCase.transfer(memberId, idempotencyKey, props)

    @GetMapping
    override fun query(
        @LoginUserId memberId: Long,
        @RequestParam accountId: Long,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        from: LocalDateTime?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        to: LocalDateTime?,
        @RequestParam(required = false) limit: Int?,
    ): TransferQueryResponse {
        val transfers = transferQueryUseCase.query(memberId, accountId, TransferQueryCondition(from, to, limit))
        return TransferQueryResponse.from(transfers)
    }
}
