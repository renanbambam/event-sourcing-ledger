package dev.renanbambam.ledger.domain.service

import dev.renanbambam.ledger.domain.command.DepositCommand
import dev.renanbambam.ledger.domain.command.OpenAccountCommand
import dev.renanbambam.ledger.domain.command.TransferCommand
import dev.renanbambam.ledger.domain.command.WithdrawCommand
import dev.renanbambam.ledger.domain.event.AccountOpened
import dev.renanbambam.ledger.domain.event.MoneyDeposited
import dev.renanbambam.ledger.domain.event.MoneyTransferred
import dev.renanbambam.ledger.domain.event.MoneyWithdrawn
import dev.renanbambam.ledger.domain.model.Account
import dev.renanbambam.ledger.domain.model.AccountSnapshot
import dev.renanbambam.ledger.domain.port.command.DepositHandler
import dev.renanbambam.ledger.domain.port.command.OpenAccountHandler
import dev.renanbambam.ledger.domain.port.command.TransferHandler
import dev.renanbambam.ledger.domain.port.command.WithdrawHandler
import dev.renanbambam.ledger.domain.port.out.EventStore
import dev.renanbambam.ledger.domain.port.out.SnapshotStore

class AccountCommandService(
    private val eventStore: EventStore,
    private val snapshotStore: SnapshotStore
) : OpenAccountHandler, DepositHandler, WithdrawHandler, TransferHandler {

    private val loader = AccountLoader(eventStore, snapshotStore)

    override fun handle(command: OpenAccountCommand): AccountOpened {
        val account = Account.open(command.ownerId, command.initialBalance)
        val event = account.flushPendingEvents().single() as AccountOpened
        eventStore.append(account.id, listOf(event), expectedVersion = 0)
        return event
    }

    override fun handle(command: DepositCommand): MoneyDeposited {
        val account = loader.load(command.accountId)
        val expectedVersion = account.version
        val event = account.deposit(command.amount)
        eventStore.append(account.id, account.flushPendingEvents(), expectedVersion)
        maybeSnapshot(account)
        return event
    }

    override fun handle(command: WithdrawCommand): MoneyWithdrawn {
        val account = loader.load(command.accountId)
        val expectedVersion = account.version
        val event = account.withdraw(command.amount)
        eventStore.append(account.id, account.flushPendingEvents(), expectedVersion)
        maybeSnapshot(account)
        return event
    }

    override fun handle(command: TransferCommand): Pair<MoneyTransferred, MoneyTransferred> {
        val source = loader.load(command.fromAccountId)
        val sourceExpectedVersion = source.version
        val outgoing = source.transferOut(command.amount, command.toAccountId)
        eventStore.append(source.id, source.flushPendingEvents(), sourceExpectedVersion)
        maybeSnapshot(source)

        val destination = loader.load(command.toAccountId)
        val destinationExpectedVersion = destination.version
        val incoming = destination.transferIn(command.amount, command.fromAccountId)
        eventStore.append(destination.id, destination.flushPendingEvents(), destinationExpectedVersion)
        maybeSnapshot(destination)

        return outgoing to incoming
    }

    private fun maybeSnapshot(account: Account) {
        if (account.version % SNAPSHOT_INTERVAL == 0L) {
            snapshotStore.save(AccountSnapshot(account.id, account.balance, account.status, account.version))
        }
    }

    companion object {
        private const val SNAPSHOT_INTERVAL = 50L
    }
}
