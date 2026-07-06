package dev.renanbambam.ledger.adapter.inbound.rest.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

data class OpenAccountRequest(
    @field:NotBlank
    val ownerId: String,

    @field:Valid
    val initialBalance: MoneyDto
)
