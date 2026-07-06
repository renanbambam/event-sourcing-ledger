package dev.renanbambam.ledger.adapter.outbound.persistence.adapter

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.renanbambam.ledger.domain.event.AccountEvent
import dev.renanbambam.ledger.domain.event.AccountOpened
import dev.renanbambam.ledger.domain.event.MoneyDeposited
import dev.renanbambam.ledger.domain.event.MoneyTransferred
import dev.renanbambam.ledger.domain.event.MoneyWithdrawn
import dev.renanbambam.ledger.domain.model.AccountId
import dev.renanbambam.ledger.domain.model.Money
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class EventSerializer {

    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(domainValueObjectsModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun serialize(event: AccountEvent): String = objectMapper.writeValueAsString(event)

    fun deserialize(eventType: String, payload: String): AccountEvent {
        val eventClass = eventTypes[eventType]
            ?: throw IllegalArgumentException("unknown event type: $eventType")
        return objectMapper.readValue(payload, eventClass)
    }

    private fun domainValueObjectsModule(): SimpleModule =
        SimpleModule()
            .addSerializer(Money::class.java, MoneyJsonSerializer())
            .addDeserializer(Money::class.java, MoneyJsonDeserializer())
            .addSerializer(AccountId::class.java, AccountIdJsonSerializer())
            .addDeserializer(AccountId::class.java, AccountIdJsonDeserializer())

    companion object {
        private val eventTypes: Map<String, Class<out AccountEvent>> = mapOf(
            AccountOpened::class.simpleName!! to AccountOpened::class.java,
            MoneyDeposited::class.simpleName!! to MoneyDeposited::class.java,
            MoneyWithdrawn::class.simpleName!! to MoneyWithdrawn::class.java,
            MoneyTransferred::class.simpleName!! to MoneyTransferred::class.java
        )
    }
}

private class MoneyJsonSerializer : com.fasterxml.jackson.databind.JsonSerializer<Money>() {
    override fun serialize(value: Money, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("amount", value.amount.toPlainString())
        gen.writeStringField("currency", value.currency)
        gen.writeEndObject()
    }
}

private class MoneyJsonDeserializer : com.fasterxml.jackson.databind.JsonDeserializer<Money>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Money {
        val node = p.codec.readTree<com.fasterxml.jackson.databind.JsonNode>(p)
        val amount = BigDecimal(node.get("amount").asText())
        val currency = node.get("currency").asText()
        return Money.of(amount, currency)
    }
}

private class AccountIdJsonSerializer : com.fasterxml.jackson.databind.JsonSerializer<AccountId>() {
    override fun serialize(value: AccountId, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.value.toString())
    }
}

private class AccountIdJsonDeserializer : com.fasterxml.jackson.databind.JsonDeserializer<AccountId>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): AccountId =
        AccountId.of(p.text)
}
