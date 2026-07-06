package dev.renanbambam.ledger.adapter.outbound.persistence.adapter

import dev.renanbambam.ledger.adapter.outbound.persistence.entity.EventEntity
import dev.renanbambam.ledger.adapter.outbound.persistence.repository.EventJpaRepository
import dev.renanbambam.ledger.domain.event.AccountEvent
import dev.renanbambam.ledger.domain.exception.ConcurrencyException
import dev.renanbambam.ledger.domain.model.AccountId
import dev.renanbambam.ledger.domain.port.out.EventStore
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PostgresEventStore(
    private val repository: EventJpaRepository,
    private val serializer: EventSerializer
) : EventStore {

    @Transactional
    override fun append(accountId: AccountId, events: List<AccountEvent>, expectedVersion: Long) {
        val currentVersion = repository.findMaxVersionByAggregateId(accountId.value.toString()) ?: 0L
        if (currentVersion != expectedVersion) {
            throw ConcurrencyException(
                "expected version $expectedVersion for account $accountId but found $currentVersion"
            )
        }

        val entities = events.map { event ->
            EventEntity(
                aggregateId = accountId.value.toString(),
                eventType = event::class.simpleName!!,
                payload = serializer.serialize(event),
                version = event.version,
                occurredAt = event.occurredAt
            )
        }

        try {
            repository.saveAll(entities)
            repository.flush()
        } catch (ex: DataIntegrityViolationException) {
            throw ConcurrencyException(
                "concurrent write detected for account $accountId at version(s) ${events.map { it.version }}"
            )
        }
    }

    override fun loadAll(accountId: AccountId): List<AccountEvent> =
        repository.findAllByAggregateIdOrderByVersionAsc(accountId.value.toString())
            .map { serializer.deserialize(it.eventType, it.payload) }

    override fun loadFrom(accountId: AccountId, fromVersion: Long): List<AccountEvent> =
        repository.findAllByAggregateIdAndVersionGreaterThanEqualOrderByVersionAsc(
            accountId.value.toString(),
            fromVersion
        ).map { serializer.deserialize(it.eventType, it.payload) }
}
