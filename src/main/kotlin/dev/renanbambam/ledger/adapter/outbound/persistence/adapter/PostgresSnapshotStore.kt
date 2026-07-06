package dev.renanbambam.ledger.adapter.outbound.persistence.adapter

import dev.renanbambam.ledger.adapter.outbound.persistence.entity.SnapshotEntity
import dev.renanbambam.ledger.adapter.outbound.persistence.repository.SnapshotJpaRepository
import dev.renanbambam.ledger.domain.model.AccountId
import dev.renanbambam.ledger.domain.model.AccountSnapshot
import dev.renanbambam.ledger.domain.model.AccountStatus
import dev.renanbambam.ledger.domain.model.Money
import dev.renanbambam.ledger.domain.port.out.SnapshotStore
import org.springframework.stereotype.Component

@Component
class PostgresSnapshotStore(
    private val repository: SnapshotJpaRepository
) : SnapshotStore {

    override fun load(accountId: AccountId): AccountSnapshot? =
        repository.findById(accountId.value.toString())
            .map { entity ->
                AccountSnapshot(
                    accountId = accountId,
                    balance = Money.of(entity.balance, entity.currency),
                    status = AccountStatus.valueOf(entity.status),
                    version = entity.version
                )
            }
            .orElse(null)

    override fun save(snapshot: AccountSnapshot) {
        val entity = SnapshotEntity(
            aggregateId = snapshot.accountId.value.toString(),
            balance = snapshot.balance.amount,
            currency = snapshot.balance.currency,
            status = snapshot.status.name,
            version = snapshot.version
        )
        repository.save(entity)
    }
}
