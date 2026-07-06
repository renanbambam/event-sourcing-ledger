package dev.renanbambam.ledger.adapter.inbound.rest.dto

import jakarta.validation.Valid

data class DepositRequest(
    @field:Valid
    val amount: MoneyDto
)
