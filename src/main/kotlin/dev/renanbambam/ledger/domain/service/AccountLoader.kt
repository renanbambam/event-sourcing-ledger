package dev.renanbambam.ledger.domain.service

import dev.renanbambam.ledger.domain.exception.AccountNotFoundException
import dev.renanbambam.ledger.domain.model.Account
import dev.renanbambam.ledger.domain.model.AccountId
import dev.renanbambam.ledger.domain.port.out.EventStore
import dev.renanbambam.ledger.domain.port.out.SnapshotStore

class AccountLoader(
    private val eventStore: EventStore,
    private val snapshotStore: SnapshotStore
) {

    fun load(accountId: AccountId): Account {
        val snapshot = snapshotStore.load(accountId)
        if (snapshot != null) {
            val eventsAfterSnapshot = eventStore.loadFrom(accountId, snapshot.version + 1)
            return Account.replayFrom(snapshot, eventsAfterSnapshot)
        }

        val events = eventStore.loadAll(accountId)
        if (events.isEmpty()) {
            throw AccountNotFoundException(accountId)
        }
        return Account.replayFrom(events)
    }
}
