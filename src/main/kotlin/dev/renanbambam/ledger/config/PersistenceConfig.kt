package dev.renanbambam.ledger.config

import dev.renanbambam.ledger.domain.port.out.EventStore
import dev.renanbambam.ledger.domain.port.out.SnapshotStore
import dev.renanbambam.ledger.domain.service.AccountCommandService
import dev.renanbambam.ledger.domain.service.AccountQueryService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PersistenceConfig {

    @Bean
    fun accountCommandService(eventStore: EventStore, snapshotStore: SnapshotStore): AccountCommandService =
        AccountCommandService(eventStore, snapshotStore)

    @Bean
    fun accountQueryService(eventStore: EventStore, snapshotStore: SnapshotStore): AccountQueryService =
        AccountQueryService(eventStore, snapshotStore)
}
