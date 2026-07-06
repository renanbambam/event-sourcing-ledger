package dev.renanbambam.ledger.adapter.inbound.rest.dto

import dev.renanbambam.ledger.domain.model.Money
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

data class MoneyDto(
    @field:DecimalMin(value = "0.0", inclusive = true)
    val amount: BigDecimal,

    @field:NotBlank
    val currency: String
)

fun MoneyDto.toMoney(): Money = Money.of(amount, currency)
