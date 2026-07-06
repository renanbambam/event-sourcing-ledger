package dev.renanbambam.ledger.domain.event

import dev.renanbambam.ledger.domain.model.AccountId
import dev.renanbambam.ledger.domain.model.Money
import java.time.Instant

data class MoneyWithdrawn(
    override val accountId: AccountId,
    val amount: Money,
    override val occurredAt: Instant = Instant.now(),
    override val version: Long
) : AccountEvent()
