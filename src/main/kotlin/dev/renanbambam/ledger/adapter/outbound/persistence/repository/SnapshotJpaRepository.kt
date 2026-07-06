package dev.renanbambam.ledger.adapter.outbound.persistence.repository

import dev.renanbambam.ledger.adapter.outbound.persistence.entity.SnapshotEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SnapshotJpaRepository : JpaRepository<SnapshotEntity, String>
