package org.yechan.remittance

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = KotlinLogging.logger {}

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<Any> {
        log.error(e) { e.stackTraceToString() }
        return ResponseEntity.status(e.status.toHttpStatus()).body(e.message ?: "")
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<Any> {
        log.error(e) { e.stackTraceToString() }
        return ResponseEntity.badRequest().body(e.message ?: "")
    }
}
