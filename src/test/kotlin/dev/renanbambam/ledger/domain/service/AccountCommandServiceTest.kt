package dev.renanbambam.ledger.domain.service

import dev.renanbambam.ledger.domain.command.DepositCommand
import dev.renanbambam.ledger.domain.command.OpenAccountCommand
import dev.renanbambam.ledger.domain.command.TransferCommand
import dev.renanbambam.ledger.domain.command.WithdrawCommand
import dev.renanbambam.ledger.domain.event.AccountEvent
import dev.renanbambam.ledger.domain.exception.ConcurrencyException
import dev.renanbambam.ledger.domain.model.Account
import dev.renanbambam.ledger.domain.model.AccountSnapshot
import dev.renanbambam.ledger.domain.model.Money
import dev.renanbambam.ledger.domain.port.out.EventStore
import dev.renanbambam.ledger.domain.port.out.SnapshotStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AccountCommandServiceTest {

    private val eventStore = mockk<EventStore>(relaxed = true)
    private val snapshotStore = mockk<SnapshotStore>()
    private val service = AccountCommandService(eventStore, snapshotStore)

    @Test
    fun `opening an account appends a single AccountOpened event at version 0`() {
        val command = OpenAccountCommand("owner-1", Money.of("100.00", "BRL"))
        val eventsSlot = slot<List<AccountEvent>>()

        every { eventStore.append(any(), capture(eventsSlot), 0L) } returns Unit

        val event = service.handle(command)

        assertEquals("owner-1", event.ownerId)
        assertEquals(1, eventsSlot.captured.size)
        assertEquals(event, eventsSlot.captured.first())
    }

    @Test
    fun `deposit loads the account and appends with the version prior to mutation`() {
        val existing = Account.open("owner-1", Money.of("100.00", "BRL"))
        val accountId = existing.id

        every { snapshotStore.load(accountId) } returns null
        every { eventStore.loadAll(accountId) } returns existing.flushPendingEvents()

        val versionSlot = slot<Long>()
        every { eventStore.append(eq(accountId), any(), capture(versionSlot)) } returns Unit

        val event = service.handle(DepositCommand(accountId, Money.of("25.00", "BRL")))

        assertEquals(Money.of("25.00", "BRL"), event.amount)
        assertEquals(1L, versionSlot.captured)
    }

    @Test
    fun `a snapshot is written every 50 events`() {
        val existing = Account.open("owner-1", Money.of("1000.00", "BRL"))
        val accountId = existing.id

        every { snapshotStore.load(accountId) } returns AccountSnapshot(
            accountId, Money.of("1000.00", "BRL"), existing.status, 49L
        )
        every { eventStore.loadFrom(accountId, 50L) } returns emptyList()
        every { eventStore.append(eq(accountId), any(), 49L) } returns Unit
        every { snapshotStore.save(any()) } returns Unit

        service.handle(DepositCommand(accountId, Money.of("10.00", "BRL")))

        verify(exactly = 1) { snapshotStore.save(match { it.version == 50L }) }
    }

    @Test
    fun `no snapshot is written when the resulting version is not a multiple of 50`() {
        val existing = Account.open("owner-1", Money.of("100.00", "BRL"))
        val accountId = existing.id

        every { snapshotStore.load(accountId) } returns null
        every { eventStore.loadAll(accountId) } returns existing.flushPendingEvents()
        every { eventStore.append(eq(accountId), any(), any()) } returns Unit

        service.handle(DepositCommand(accountId, Money.of("10.00", "BRL")))

        verify(exactly = 0) { snapshotStore.save(any()) }
    }

    @Test
    fun `a version mismatch on append propagates as ConcurrencyException`() {
        val existing = Account.open("owner-1", Money.of("100.00", "BRL"))
        val accountId = existing.id

        every { snapshotStore.load(accountId) } returns null
        every { eventStore.loadAll(accountId) } returns existing.flushPendingEvents()
        every { eventStore.append(eq(accountId), any(), any()) } throws ConcurrencyException("boom")

        assertThrows(ConcurrencyException::class.java) {
            service.handle(WithdrawCommand(accountId, Money.of("10.00", "BRL")))
        }
    }

    @Test
    fun `transfer appends an event on both the source and destination streams`() {
        val source = Account.open("owner-1", Money.of("100.00", "BRL"))
        val destination = Account.open("owner-2", Money.of("0.00", "BRL"))

        every { snapshotStore.load(source.id) } returns null
        every { eventStore.loadAll(source.id) } returns source.flushPendingEvents()
        every { snapshotStore.load(destination.id) } returns null
        every { eventStore.loadAll(destination.id) } returns destination.flushPendingEvents()
        every { eventStore.append(any(), any(), any()) } returns Unit

        val (outgoing, incoming) = service.handle(
            TransferCommand(source.id, destination.id, Money.of("40.00", "BRL"))
        )

        assertEquals(source.id, outgoing.accountId)
        assertEquals(destination.id, incoming.accountId)
        verify(exactly = 1) { eventStore.append(source.id, any(), 1L) }
        verify(exactly = 1) { eventStore.append(destination.id, any(), 1L) }
    }
}
