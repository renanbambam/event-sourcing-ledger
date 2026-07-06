package dev.renanbambam.ledger.adapter.inbound.rest.dto

import jakarta.validation.Valid

data class WithdrawRequest(
    @field:Valid
    val amount: MoneyDto
)
