package dev.renanbambam.ledger.domain.service

import dev.renanbambam.ledger.domain.event.AccountEvent
import dev.renanbambam.ledger.domain.exception.AccountNotFoundException
import dev.renanbambam.ledger.domain.model.AccountId
import dev.renanbambam.ledger.domain.model.Money
import dev.renanbambam.ledger.domain.port.out.EventStore
import dev.renanbambam.ledger.domain.port.out.SnapshotStore
import dev.renanbambam.ledger.domain.port.query.GetAccountBalanceQuery
import dev.renanbambam.ledger.domain.port.query.GetAccountHistoryQuery

class AccountQueryService(
    private val eventStore: EventStore,
    snapshotStore: SnapshotStore
) : GetAccountBalanceQuery, GetAccountHistoryQuery {

    private val loader = AccountLoader(eventStore, snapshotStore)

    override fun getBalance(accountId: AccountId): Money {
        return loader.load(accountId).balance
    }

    override fun getHistory(accountId: AccountId): List<AccountEvent> {
        val events = eventStore.loadAll(accountId)
        if (events.isEmpty()) {
            throw AccountNotFoundException(accountId)
        }
        return events
    }
}
