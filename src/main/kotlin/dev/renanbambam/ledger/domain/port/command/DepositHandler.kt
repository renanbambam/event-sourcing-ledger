package dev.renanbambam.ledger.domain.port.command

import dev.renanbambam.ledger.domain.command.DepositCommand
import dev.renanbambam.ledger.domain.event.MoneyDeposited

interface DepositHandler {
    fun handle(command: DepositCommand): MoneyDeposited
}
