package dev.renanbambam.ledger.adapter.inbound.rest.dto

import java.time.Instant

data class EventHistoryResponse(
    val accountId: String,
    val events: List<EventDto>
)

data class EventDto(
    val type: String,
    val version: Long,
    val occurredAt: Instant,
    val details: Map<String, Any?>
)
