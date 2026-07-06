package dev.renanbambam.ledger.domain.port.query

import dev.renanbambam.ledger.domain.model.AccountId
import dev.renanbambam.ledger.domain.model.Money

interface GetAccountBalanceQuery {
    fun getBalance(accountId: AccountId): Money
}
