package dev.renanbambam.ledger.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AccountTest {

    @Test
    fun `opening an account sets the initial balance and version 1`() {
        val account = Account.open("owner-1", Money.of("100.00", "BRL"))

        assertEquals(Money.of("100.00", "BRL"), account.balance)
        assertEquals(1L, account.version)
        assertEquals(AccountStatus.ACTIVE, account.status)
    }

    @Test
    fun `deposit increases balance and bumps version`() {
        val account = Account.open("owner-1", Money.of("100.00", "BRL"))
        account.flushPendingEvents()

        account.deposit(Money.of("50.00", "BRL"))

        assertEquals(Money.of("150.00", "BRL"), account.balance)
        assertEquals(2L, account.version)
    }

    @Test
    fun `withdraw decreases balance`() {
        val account = Account.open("owner-1", Money.of("100.00", "BRL"))

        account.withdraw(Money.of("30.00", "BRL"))

        assertEquals(Money.of("70.00", "BRL"), account.balance)
    }

    @Test
    fun `withdraw beyond balance is rejected`() {
        val account = Account.open("owner-1", Money.of("100.00", "BRL"))

        assertThrows(IllegalArgumentException::class.java) {
            account.withdraw(Money.of("200.00", "BRL"))
        }
    }

    @Test
    fun `deposit of a non-positive amount is rejected`() {
        val account = Account.open("owner-1", Money.of("100.00", "BRL"))

        assertThrows(IllegalArgumentException::class.java) {
            account.deposit(Money.zero("BRL"))
        }
    }

    @Test
    fun `transferOut and transferIn move balance on each side`() {
        val source = Account.open("owner-1", Money.of("100.00", "BRL"))
        val destination = Account.open("owner-2", Money.of("0.00", "BRL"))

        source.transferOut(Money.of("40.00", "BRL"), destination.id)
        destination.transferIn(Money.of("40.00", "BRL"), source.id)

        assertEquals(Money.of("60.00", "BRL"), source.balance)
        assertEquals(Money.of("40.00", "BRL"), destination.balance)
    }

    @Test
    fun `transferOut beyond balance is rejected`() {
        val source = Account.open("owner-1", Money.of("100.00", "BRL"))
        val destination = Account.open("owner-2", Money.of("0.00", "BRL"))

        assertThrows(IllegalArgumentException::class.java) {
            source.transferOut(Money.of("500.00", "BRL"), destination.id)
        }
    }

    @Test
    fun `replayFrom reconstructs balance and version from a full event stream`() {
        val account = Account.open("owner-1", Money.of("100.00", "BRL"))
        account.deposit(Money.of("50.00", "BRL"))
        account.withdraw(Money.of("20.00", "BRL"))
        val events = account.flushPendingEvents()

        val replayed = Account.replayFrom(events)

        assertEquals(Money.of("130.00", "BRL"), replayed.balance)
        assertEquals(3L, replayed.version)
        assertEquals(account.id, replayed.id)
    }

    @Test
    fun `replayFrom a snapshot only replays events after the snapshot version`() {
        val account = Account.open("owner-1", Money.of("100.00", "BRL"))
        account.deposit(Money.of("50.00", "BRL"))
        account.flushPendingEvents()

        val snapshot = AccountSnapshot(account.id, account.balance, account.status, account.version)

        account.withdraw(Money.of("30.00", "BRL"))
        val eventsAfterSnapshot = account.flushPendingEvents()

        val replayed = Account.replayFrom(snapshot, eventsAfterSnapshot)

        assertEquals(Money.of("120.00", "BRL"), replayed.balance)
        assertEquals(3L, replayed.version)
    }

    @Test
    fun `replayFrom rejects an empty event list`() {
        assertThrows(IllegalArgumentException::class.java) {
            Account.replayFrom(emptyList())
        }
    }

    @Test
    fun `operating on a closed account is rejected`() {
        val account = Account.open("owner-1", Money.of("100.00", "BRL"))
        val closed = Account.replayFrom(
            AccountSnapshot(account.id, account.balance, AccountStatus.CLOSED, account.version),
            emptyList()
        )

        assertThrows(IllegalStateException::class.java) {
            closed.deposit(Money.of("10.00", "BRL"))
        }
    }
}
