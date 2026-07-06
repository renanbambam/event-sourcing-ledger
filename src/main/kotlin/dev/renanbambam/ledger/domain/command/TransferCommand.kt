package dev.renanbambam.ledger.domain.command

import dev.renanbambam.ledger.domain.model.AccountId
import dev.renanbambam.ledger.domain.model.Money

data class TransferCommand(
    val fromAccountId: AccountId,
    val toAccountId: AccountId,
    val amount: Money
)
