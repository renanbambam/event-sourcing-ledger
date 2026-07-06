package dev.renanbambam.ledger.domain.model

data class AccountSnapshot(
    val accountId: AccountId,
    val balance: Money,
    val status: AccountStatus,
    val version: Long
)
