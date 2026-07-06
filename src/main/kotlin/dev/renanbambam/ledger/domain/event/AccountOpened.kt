package dev.renanbambam.ledger.domain.event

import dev.renanbambam.ledger.domain.model.AccountId
import dev.renanbambam.ledger.domain.model.Money
import java.time.Instant

data class AccountOpened(
    override val accountId: AccountId,
    val ownerId: String,
    val initialBalance: Money,
    override val occurredAt: Instant = Instant.now(),
    override val version: Long = 1
) : AccountEvent()
