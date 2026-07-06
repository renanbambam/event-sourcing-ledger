package dev.renanbambam.ledger.adapter.inbound.rest

import dev.renanbambam.ledger.adapter.inbound.rest.dto.BalanceResponse
import dev.renanbambam.ledger.adapter.inbound.rest.dto.DepositRequest
import dev.renanbambam.ledger.adapter.inbound.rest.dto.MoneyDto
import dev.renanbambam.ledger.adapter.inbound.rest.dto.OpenAccountRequest
import dev.renanbambam.ledger.adapter.inbound.rest.dto.TransferRequest
import dev.renanbambam.ledger.adapter.inbound.rest.dto.WithdrawRequest
import dev.renanbambam.ledger.adapter.inbound.rest.dto.toMoney
import dev.renanbambam.ledger.domain.command.DepositCommand
import dev.renanbambam.ledger.domain.command.OpenAccountCommand
import dev.renanbambam.ledger.domain.command.TransferCommand
import dev.renanbambam.ledger.domain.command.WithdrawCommand
import dev.renanbambam.ledger.domain.model.AccountId
import dev.renanbambam.ledger.domain.port.command.DepositHandler
import dev.renanbambam.ledger.domain.port.command.OpenAccountHandler
import dev.renanbambam.ledger.domain.port.command.TransferHandler
import dev.renanbambam.ledger.domain.port.command.WithdrawHandler
import dev.renanbambam.ledger.domain.port.query.GetAccountBalanceQuery
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/accounts")
class AccountCommandController(
    private val openAccountHandler: OpenAccountHandler,
    private val depositHandler: DepositHandler,
    private val withdrawHandler: WithdrawHandler,
    private val transferHandler: TransferHandler,
    private val getAccountBalanceQuery: GetAccountBalanceQuery
) {

    @PostMapping
    fun openAccount(@Valid @RequestBody request: OpenAccountRequest): ResponseEntity<BalanceResponse> {
        val event = openAccountHandler.handle(
            OpenAccountCommand(request.ownerId, request.initialBalance.toMoney())
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(balanceResponse(event.accountId))
    }

    @PostMapping("/{id}/deposit")
    fun deposit(@PathVariable id: String, @Valid @RequestBody request: DepositRequest): ResponseEntity<BalanceResponse> {
        val accountId = AccountId.of(id)
        depositHandler.handle(DepositCommand(accountId, request.amount.toMoney()))
        return ResponseEntity.ok(balanceResponse(accountId))
    }

    @PostMapping("/{id}/withdraw")
    fun withdraw(@PathVariable id: String, @Valid @RequestBody request: WithdrawRequest): ResponseEntity<BalanceResponse> {
        val accountId = AccountId.of(id)
        withdrawHandler.handle(WithdrawCommand(accountId, request.amount.toMoney()))
        return ResponseEntity.ok(balanceResponse(accountId))
    }

    @PostMapping("/{id}/transfer")
    fun transfer(@PathVariable id: String, @Valid @RequestBody request: TransferRequest): ResponseEntity<BalanceResponse> {
        val fromAccountId = AccountId.of(id)
        val toAccountId = AccountId.of(request.toAccountId)
        transferHandler.handle(TransferCommand(fromAccountId, toAccountId, request.amount.toMoney()))
        return ResponseEntity.ok(balanceResponse(fromAccountId))
    }

    private fun balanceResponse(accountId: AccountId): BalanceResponse {
        val balance = getAccountBalanceQuery.getBalance(accountId)
        return BalanceResponse(accountId.toString(), MoneyDto(balance.amount, balance.currency))
    }
}
