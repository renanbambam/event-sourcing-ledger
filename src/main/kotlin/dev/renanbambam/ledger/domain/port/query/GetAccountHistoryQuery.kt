package dev.renanbambam.ledger.domain.port.query

import dev.renanbambam.ledger.domain.event.AccountEvent
import dev.renanbambam.ledger.domain.model.AccountId

interface GetAccountHistoryQuery {
    fun getHistory(accountId: AccountId): List<AccountEvent>
}
