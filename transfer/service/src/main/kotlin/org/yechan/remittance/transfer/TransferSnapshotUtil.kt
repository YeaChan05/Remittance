package org.yechan.remittance.transfer

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.time.LocalDateTime

class TransferSnapshotUtil(
    private val objectMapper: ObjectMapper
) {
    fun toSnapshot(result: TransferResult): String {
        return writeJson(
            mapOf(
                "status" to result.status.name,
                "transferId" to result.transferId,
                "errorCode" to result.errorCode
            )
        )
    }

    fun fromSnapshot(snapshot: String): TransferResult {
        if (snapshot.isBlank()) {
            throw IllegalArgumentException("Snapshot is empty")
        }
        try {
            val payload = objectMapper.readTree(snapshot)
            val status = TransferProps.TransferStatusValue.valueOf(payload["status"].asText())
            val transferId =
                if (payload["transferId"] == null || payload["transferId"].isNull) {
                    null
                } else {
                    payload["transferId"].asLong()
                }
            val errorCode =
                if (payload["errorCode"] == null || payload["errorCode"].isNull) {
                    null
                } else {
                    payload["errorCode"].asText()
                }
            return TransferResult(status, transferId, errorCode)
        } catch (_: JsonProcessingException) {
            throw TransferException("Json deserialization failed")
        }
    }

    fun toHashRequest(props: TransferRequestProps): String {
        val canonical =
            writeJson(
                linkedMapOf(
                    "fromAccountId" to props.fromAccountId,
                    "toAccountId" to props.toAccountId,
                    "amount" to props.amount
                )
            )
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashed = digest.digest(canonical.toByteArray(StandardCharsets.UTF_8))
            return toHex(hashed)
        } catch (_: NoSuchAlgorithmException) {
            throw TransferException("Hash algorithm not found")
        }
    }

    fun toOutboxPayload(
        transfer: TransferModel,
        props: TransferRequestProps,
        now: LocalDateTime
    ): String {
        return writeJson(
            linkedMapOf(
                "transferId" to transfer.transferId,
                "fromAccountId" to props.fromAccountId,
                "toAccountId" to props.toAccountId,
                "amount" to props.amount,
                "completedAt" to now.toString()
            )
        )
    }

    private fun writeJson(payload: Any): String {
        try {
            return objectMapper.writeValueAsString(payload)
        } catch (_: JsonProcessingException) {
            throw TransferException("Json serialization failed")
        }
    }

    private fun toHex(bytes: ByteArray): String {
        val builder = StringBuilder(bytes.size * 2)
        for (value in bytes) {
            builder.append(String.format("%02x", value))
        }
        return builder.toString()
    }
}
