package dev.renanbambam.ledger.domain.port.command

import dev.renanbambam.ledger.domain.command.WithdrawCommand
import dev.renanbambam.ledger.domain.event.MoneyWithdrawn

interface WithdrawHandler {
    fun handle(command: WithdrawCommand): MoneyWithdrawn
}
