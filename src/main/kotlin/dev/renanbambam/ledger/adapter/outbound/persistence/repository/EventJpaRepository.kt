package dev.renanbambam.ledger.adapter.outbound.persistence.repository

import dev.renanbambam.ledger.adapter.outbound.persistence.entity.EventEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface EventJpaRepository : JpaRepository<EventEntity, UUID> {

    fun findAllByAggregateIdOrderByVersionAsc(aggregateId: String): List<EventEntity>

    fun findAllByAggregateIdAndVersionGreaterThanEqualOrderByVersionAsc(
        aggregateId: String,
        version: Long
    ): List<EventEntity>

    @Query("select max(e.version) from EventEntity e where e.aggregateId = :aggregateId")
    fun findMaxVersionByAggregateId(aggregateId: String): Long?
}
