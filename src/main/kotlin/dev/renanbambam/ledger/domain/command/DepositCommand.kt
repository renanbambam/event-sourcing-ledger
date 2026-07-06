package dev.renanbambam.ledger.domain.command

import dev.renanbambam.ledger.domain.model.AccountId
import dev.renanbambam.ledger.domain.model.Money

data class DepositCommand(
    val accountId: AccountId,
    val amount: Money
)
