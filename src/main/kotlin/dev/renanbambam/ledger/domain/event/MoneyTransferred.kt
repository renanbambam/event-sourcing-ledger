package dev.renanbambam.ledger.domain.event

import dev.renanbambam.ledger.domain.model.AccountId
import dev.renanbambam.ledger.domain.model.Money
import java.time.Instant

enum class TransferDirection {
    OUTGOING,
    INCOMING
}

data class MoneyTransferred(
    override val accountId: AccountId,
    val counterpartyAccountId: AccountId,
    val amount: Money,
    val direction: TransferDirection,
    override val occurredAt: Instant = Instant.now(),
    override val version: Long
) : AccountEvent()
