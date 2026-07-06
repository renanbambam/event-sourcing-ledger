package dev.renanbambam.ledger.domain.port.command

import dev.renanbambam.ledger.domain.command.TransferCommand
import dev.renanbambam.ledger.domain.event.MoneyTransferred

interface TransferHandler {
    fun handle(command: TransferCommand): Pair<MoneyTransferred, MoneyTransferred>
}
