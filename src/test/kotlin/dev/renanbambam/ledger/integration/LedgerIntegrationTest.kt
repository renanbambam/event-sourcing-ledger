package dev.renanbambam.ledger.integration

import dev.renanbambam.ledger.adapter.outbound.persistence.repository.SnapshotJpaRepository
import dev.renanbambam.ledger.domain.command.DepositCommand
import dev.renanbambam.ledger.domain.command.OpenAccountCommand
import dev.renanbambam.ledger.domain.command.WithdrawCommand
import dev.renanbambam.ledger.domain.event.MoneyDeposited
import dev.renanbambam.ledger.domain.exception.ConcurrencyException
import dev.renanbambam.ledger.domain.model.Money
import dev.renanbambam.ledger.domain.port.command.DepositHandler
import dev.renanbambam.ledger.domain.port.command.OpenAccountHandler
import dev.renanbambam.ledger.domain.port.command.WithdrawHandler
import dev.renanbambam.ledger.domain.port.out.EventStore
import dev.renanbambam.ledger.domain.port.query.GetAccountBalanceQuery
import dev.renanbambam.ledger.domain.port.query.GetAccountHistoryQuery
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Tag("integration")
@Testcontainers
@SpringBootTest
class LedgerIntegrationTest {

    @Autowired
    private lateinit var openAccountHandler: OpenAccountHandler

    @Autowired
    private lateinit var depositHandler: DepositHandler

    @Autowired
    private lateinit var withdrawHandler: WithdrawHandler

    @Autowired
    private lateinit var getAccountBalanceQuery: GetAccountBalanceQuery

    @Autowired
    private lateinit var getAccountHistoryQuery: GetAccountHistoryQuery

    @Autowired
    private lateinit var eventStore: EventStore

    @Autowired
    private lateinit var snapshotJpaRepository: SnapshotJpaRepository

    @Test
    fun `deposits and withdrawals are reflected in the replayed balance`() {
        val opened = openAccountHandler.handle(OpenAccountCommand("owner-1", Money.of("100.00", "BRL")))

        depositHandler.handle(DepositCommand(opened.accountId, Money.of("50.00", "BRL")))
        withdrawHandler.handle(WithdrawCommand(opened.accountId, Money.of("30.00", "BRL")))

        val balance = getAccountBalanceQuery.getBalance(opened.accountId)
        assertEquals(Money.of("120.00", "BRL"), balance)

        val history = getAccountHistoryQuery.getHistory(opened.accountId)
        assertEquals(3, history.size)
    }

    @Test
    fun `a snapshot row is written once the version reaches 50`() {
        val opened = openAccountHandler.handle(OpenAccountCommand("owner-2", Money.of("0.00", "BRL")))

        repeat(49) {
            depositHandler.handle(DepositCommand(opened.accountId, Money.of("1.00", "BRL")))
        }

        val snapshotRow = snapshotJpaRepository.findById(opened.accountId.value.toString())
        assertTrue(snapshotRow.isPresent)
        assertEquals(50L, snapshotRow.get().version)
        assertEquals(Money.of("49.00", "BRL"), getAccountBalanceQuery.getBalance(opened.accountId))
    }

    @Test
    fun `appending with a stale expected version throws ConcurrencyException`() {
        val opened = openAccountHandler.handle(OpenAccountCommand("owner-3", Money.of("10.00", "BRL")))

        assertThrows(ConcurrencyException::class.java) {
            eventStore.append(
                opened.accountId,
                listOf(MoneyDeposited(opened.accountId, Money.of("5.00", "BRL"), version = 2)),
                expectedVersion = 0L
            )
        }
    }

    companion object {
        @Container
        @JvmStatic
        private val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("ledger")
            .withUsername("ledger")
            .withPassword("ledger")

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
