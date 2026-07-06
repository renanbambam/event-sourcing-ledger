package dev.renanbambam.ledger.domain.model

import java.util.UUID

data class AccountId(val value: UUID) {

    override fun toString(): String = value.toString()

    companion object {
        fun new(): AccountId = AccountId(UUID.randomUUID())

        fun of(raw: String): AccountId = AccountId(UUID.fromString(raw))
    }
}
