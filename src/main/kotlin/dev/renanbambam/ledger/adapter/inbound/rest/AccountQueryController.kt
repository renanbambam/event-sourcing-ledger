package dev.renanbambam.ledger.adapter.inbound.rest

import dev.renanbambam.ledger.adapter.inbound.rest.dto.BalanceResponse
import dev.renanbambam.ledger.adapter.inbound.rest.dto.EventDto
import dev.renanbambam.ledger.adapter.inbound.rest.dto.EventHistoryResponse
import dev.renanbambam.ledger.adapter.inbound.rest.dto.MoneyDto
import dev.renanbambam.ledger.domain.event.AccountEvent
import dev.renanbambam.ledger.domain.event.AccountOpened
import dev.renanbambam.ledger.domain.event.MoneyDeposited
import dev.renanbambam.ledger.domain.event.MoneyTransferred
import dev.renanbambam.ledger.domain.event.MoneyWithdrawn
import dev.renanbambam.ledger.domain.model.AccountId
import dev.renanbambam.ledger.domain.port.query.GetAccountBalanceQuery
import dev.renanbambam.ledger.domain.port.query.GetAccountHistoryQuery
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/accounts")
class AccountQueryController(
    private val getAccountBalanceQuery: GetAccountBalanceQuery,
    private val getAccountHistoryQuery: GetAccountHistoryQuery
) {

    @GetMapping("/{id}/balance")
    fun balance(@PathVariable id: String): BalanceResponse {
        val accountId = AccountId.of(id)
        val balance = getAccountBalanceQuery.getBalance(accountId)
        return BalanceResponse(accountId.toString(), MoneyDto(balance.amount, balance.currency))
    }

    @GetMapping("/{id}/history")
    fun history(@PathVariable id: String): EventHistoryResponse {
        val accountId = AccountId.of(id)
        val events = getAccountHistoryQuery.getHistory(accountId)
        return EventHistoryResponse(accountId.toString(), events.map { it.toDto() })
    }

    private fun AccountEvent.toDto(): EventDto = when (this) {
        is AccountOpened -> EventDto(
            type = "AccountOpened",
            version = version,
            occurredAt = occurredAt,
            details = mapOf(
                "ownerId" to ownerId,
                "initialBalance" to "${initialBalance.amount} ${initialBalance.currency}"
            )
        )
        is MoneyDeposited -> EventDto(
            type = "MoneyDeposited",
            version = version,
            occurredAt = occurredAt,
            details = mapOf("amount" to "${amount.amount} ${amount.currency}")
        )
        is MoneyWithdrawn -> EventDto(
            type = "MoneyWithdrawn",
            version = version,
            occurredAt = occurredAt,
            details = mapOf("amount" to "${amount.amount} ${amount.currency}")
        )
        is MoneyTransferred -> EventDto(
            type = "MoneyTransferred",
            version = version,
            occurredAt = occurredAt,
            details = mapOf(
                "amount" to "${amount.amount} ${amount.currency}",
                "direction" to direction.name,
                "counterpartyAccountId" to counterpartyAccountId.toString()
            )
        )
    }
}
