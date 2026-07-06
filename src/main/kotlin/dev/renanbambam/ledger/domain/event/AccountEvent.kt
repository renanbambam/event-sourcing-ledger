package dev.renanbambam.ledger.domain.event

import dev.renanbambam.ledger.domain.model.AccountId
import java.time.Instant

sealed class AccountEvent {
    abstract val accountId: AccountId
    abstract val occurredAt: Instant
    abstract val version: Long
}
