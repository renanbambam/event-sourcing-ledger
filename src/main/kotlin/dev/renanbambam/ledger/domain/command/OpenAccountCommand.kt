package dev.renanbambam.ledger.domain.command

import dev.renanbambam.ledger.domain.model.Money

data class OpenAccountCommand(
    val ownerId: String,
    val initialBalance: Money
)
