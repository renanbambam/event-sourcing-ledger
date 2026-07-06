package dev.renanbambam.ledger.adapter.inbound.rest.dto

data class BalanceResponse(
    val accountId: String,
    val balance: MoneyDto
)
