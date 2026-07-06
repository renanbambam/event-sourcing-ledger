package dev.renanbambam.ledger.adapter.inbound.rest.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

data class TransferRequest(
    @field:NotBlank
    val toAccountId: String,

    @field:Valid
    val amount: MoneyDto
)
