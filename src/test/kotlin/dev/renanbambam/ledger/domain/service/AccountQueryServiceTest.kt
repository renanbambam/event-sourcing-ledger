package dev.renanbambam.ledger.domain.service

import dev.renanbambam.ledger.domain.exception.AccountNotFoundException
import dev.renanbambam.ledger.domain.model.Account
import dev.renanbambam.ledger.domain.model.AccountId
import dev.renanbambam.ledger.domain.model.Money
import dev.renanbambam.ledger.domain.port.out.EventStore
import dev.renanbambam.ledger.domain.port.out.SnapshotStore
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AccountQueryServiceTest {

    private val eventStore = mockk<EventStore>()
    private val snapshotStore = mockk<SnapshotStore>()
    private val service = AccountQueryService(eventStore, snapshotStore)

    @Test
    fun `getBalance replays events and returns the current balance`() {
        val account = Account.open("owner-1", Money.of("100.00", "BRL"))
        account.deposit(Money.of("50.00", "BRL"))
        val events = account.flushPendingEvents()

        every { snapshotStore.load(account.id) } returns null
        every { eventStore.loadAll(account.id) } returns events

        val balance = service.getBalance(account.id)

        assertEquals(Money.of("150.00", "BRL"), balance)
    }

    @Test
    fun `getBalance for an unknown account throws AccountNotFoundException`() {
        val accountId = AccountId.new()

        every { snapshotStore.load(accountId) } returns null
        every { eventStore.loadAll(accountId) } returns emptyList()

        assertThrows(AccountNotFoundException::class.java) {
            service.getBalance(accountId)
        }
    }

    @Test
    fun `getHistory returns the full event stream in order`() {
        val account = Account.open("owner-1", Money.of("100.00", "BRL"))
        account.deposit(Money.of("50.00", "BRL"))
        val events = account.flushPendingEvents()

        every { eventStore.loadAll(account.id) } returns events

        val history = service.getHistory(account.id)

        assertEquals(events, history)
    }

    @Test
    fun `getHistory for an unknown account throws AccountNotFoundException`() {
        val accountId = AccountId.new()

        every { eventStore.loadAll(accountId) } returns emptyList()

        assertThrows(AccountNotFoundException::class.java) {
            service.getHistory(accountId)
        }
    }
}
