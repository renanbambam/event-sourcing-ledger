package dev.renanbambam.ledger.domain.port.command

import dev.renanbambam.ledger.domain.command.OpenAccountCommand
import dev.renanbambam.ledger.domain.event.AccountOpened

interface OpenAccountHandler {
    fun handle(command: OpenAccountCommand): AccountOpened
}
