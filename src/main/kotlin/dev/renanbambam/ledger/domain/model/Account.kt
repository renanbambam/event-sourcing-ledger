package dev.renanbambam.ledger.domain.model

import dev.renanbambam.ledger.domain.event.AccountEvent
import dev.renanbambam.ledger.domain.event.AccountOpened
import dev.renanbambam.ledger.domain.event.MoneyDeposited
import dev.renanbambam.ledger.domain.event.MoneyTransferred
import dev.renanbambam.ledger.domain.event.MoneyWithdrawn
import dev.renanbambam.ledger.domain.event.TransferDirection

class Account private constructor(val id: AccountId) {

    lateinit var balance: Money
        private set
    var status: AccountStatus = AccountStatus.ACTIVE
        private set
    var version: Long = 0
        private set

    private val pendingEvents = mutableListOf<AccountEvent>()

    fun deposit(amount: Money): MoneyDeposited {
        requireActive()
        require(amount.isPositive()) { "deposit amount must be positive" }
        val event = MoneyDeposited(id, amount, version = version + 1)
        apply(event)
        pendingEvents.add(event)
        return event
    }

    fun withdraw(amount: Money): MoneyWithdrawn {
        requireActive()
        require(amount.isPositive()) { "withdrawal amount must be positive" }
        require(balance >= amount) { "insufficient balance" }
        val event = MoneyWithdrawn(id, amount, version = version + 1)
        apply(event)
        pendingEvents.add(event)
        return event
    }

    fun transferOut(amount: Money, toAccountId: AccountId): MoneyTransferred {
        requireActive()
        require(amount.isPositive()) { "transfer amount must be positive" }
        require(balance >= amount) { "insufficient balance" }
        require(toAccountId != id) { "cannot transfer to the same account" }
        val event = MoneyTransferred(id, toAccountId, amount, TransferDirection.OUTGOING, version = version + 1)
        apply(event)
        pendingEvents.add(event)
        return event
    }

    fun transferIn(amount: Money, fromAccountId: AccountId): MoneyTransferred {
        requireActive()
        val event = MoneyTransferred(id, fromAccountId, amount, TransferDirection.INCOMING, version = version + 1)
        apply(event)
        pendingEvents.add(event)
        return event
    }

    private fun requireActive() {
        check(status == AccountStatus.ACTIVE) { "account $id is not active (status=$status)" }
    }

    fun apply(event: AccountEvent) {
        when (event) {
            is AccountOpened -> {
                balance = event.initialBalance
                status = AccountStatus.ACTIVE
                version = event.version
            }
            is MoneyDeposited -> {
                balance = balance + event.amount
                version = event.version
            }
            is MoneyWithdrawn -> {
                balance = balance - event.amount
                version = event.version
            }
            is MoneyTransferred -> {
                balance = when (event.direction) {
                    TransferDirection.OUTGOING -> balance - event.amount
                    TransferDirection.INCOMING -> balance + event.amount
                }
                version = event.version
            }
        }
    }

    fun flushPendingEvents(): List<AccountEvent> {
        val events = pendingEvents.toList()
        pendingEvents.clear()
        return events
    }

    companion object {
        fun open(ownerId: String, initialBalance: Money): Account {
            val account = Account(AccountId.new())
            val event = AccountOpened(account.id, ownerId, initialBalance)
            account.apply(event)
            account.pendingEvents.add(event)
            return account
        }

        fun replayFrom(events: List<AccountEvent>): Account {
            require(events.isNotEmpty()) { "cannot replay an account from an empty event list" }
            val first = events.first()
            require(first is AccountOpened) {
                "first event in the stream must be AccountOpened, got ${first::class.simpleName}"
            }
            val account = Account(first.accountId)
            events.forEach { account.apply(it) }
            return account
        }

        fun replayFrom(snapshot: AccountSnapshot, events: List<AccountEvent>): Account {
            val account = Account(snapshot.accountId)
            account.balance = snapshot.balance
            account.status = snapshot.status
            account.version = snapshot.version
            events.forEach { account.apply(it) }
            return account
        }
    }
}
