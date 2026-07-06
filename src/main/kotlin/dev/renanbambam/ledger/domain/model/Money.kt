package dev.renanbambam.ledger.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

class Money private constructor(val amount: BigDecimal, val currency: String) : Comparable<Money> {

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(amount + other.amount, currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(amount - other.amount, currency)
    }

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return amount.compareTo(other.amount)
    }

    fun isPositive(): Boolean = amount > BigDecimal.ZERO

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "cannot operate on Money with different currencies: $currency vs ${other.currency}"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Money) return false
        return amount == other.amount && currency == other.currency
    }

    override fun hashCode(): Int = amount.hashCode() * 31 + currency.hashCode()

    override fun toString(): String = "$amount $currency"

    companion object {
        fun of(amount: BigDecimal, currency: String): Money {
            require(currency.length == 3) { "currency must be a 3-letter ISO 4217 code, got '$currency'" }
            return Money(amount.setScale(2, RoundingMode.HALF_EVEN), currency)
        }

        fun of(amount: String, currency: String): Money = of(BigDecimal(amount), currency)

        fun zero(currency: String): Money = of(BigDecimal.ZERO, currency)
    }
}
