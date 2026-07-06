package dev.renanbambam.ledger.domain.exception

import dev.renanbambam.ledger.domain.model.AccountId

class AccountNotFoundException(accountId: AccountId) :
    RuntimeException("account $accountId not found")
