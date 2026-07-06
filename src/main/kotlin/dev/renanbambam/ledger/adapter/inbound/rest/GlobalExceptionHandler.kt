package dev.renanbambam.ledger.adapter.inbound.rest

import dev.renanbambam.ledger.adapter.inbound.rest.dto.ErrorResponse
import dev.renanbambam.ledger.domain.exception.AccountNotFoundException
import dev.renanbambam.ledger.domain.exception.ConcurrencyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException::class)
    fun handleAccountNotFound(ex: AccountNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(ex.message ?: "account not found"))

    @ExceptionHandler(ConcurrencyException::class)
    fun handleConcurrency(ex: ConcurrencyException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(ex.message ?: "concurrent modification"))

    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun handleInvalidRequest(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(ErrorResponse(ex.message ?: "invalid request"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.badRequest().body(ErrorResponse(message))
    }
}
