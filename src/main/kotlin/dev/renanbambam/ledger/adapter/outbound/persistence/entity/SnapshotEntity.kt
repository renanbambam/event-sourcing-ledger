package dev.renanbambam.ledger.adapter.outbound.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "account_snapshots")
class SnapshotEntity(
    @Id
    @Column(name = "aggregate_id", nullable = false, length = 36)
    val aggregateId: String,

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    val balance: BigDecimal,

    @Column(name = "currency", nullable = false, length = 3)
    val currency: String,

    @Column(name = "status", nullable = false, length = 20)
    val status: String,

    @Column(name = "version", nullable = false)
    val version: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
