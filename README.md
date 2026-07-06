# event-sourcing-ledger

A financial account ledger built on Event Sourcing: the balance is never stored as a
column, it's always derived by replaying the account's events.

I built this to get comfortable with the pattern banks and fintechs use for ledgers,
instead of the CRUD-style "update the balance column" approach I've used in most
Spring Boot projects. The interesting part wasn't the happy path, it was thinking
through what happens when two requests hit the same account at the same time, and
how you recover the balance at an arbitrary point in the past without ever trusting
a cached number.

## How it works

Every operation on an account (open, deposit, withdraw, transfer) produces one or
more immutable events. The `Account` aggregate has no persisted state of its own -
`Account.replayFrom(events)` folds a list of events into a balance, a status and a
version, starting from nothing. Loading an account for a command always goes through
this replay, never through a stored "current balance" field.

A transfer between two accounts writes a `MoneyTransferred` event on each side
(`OUTGOING` on the source, `INCOMING` on the destination), because the source and
destination are separate aggregates with separate event streams - there's no single
event that can update two rows atomically here, so the two appends happen
sequentially. That's a real limitation, not an implementation shortcut; see below.

CQRS separates the write side (`AccountCommandService`, which loads an aggregate,
mutates it, and appends the resulting events) from the read side
(`AccountQueryService`, which only replays). They share the same Postgres database
in this project - splitting them onto separate stores would be the next step if the
read load ever became a bottleneck.

Snapshots are written every 50 events (`AccountCommandService.maybeSnapshot`) so an
account with a long history doesn't need a full replay from event #1 on every load.
`Account.replayFrom(snapshot, events)` picks up from the snapshot and only replays
what happened after it.

## Optimistic locking

`PostgresEventStore.append` checks the highest version already persisted for the
aggregate against the version the caller expected before touching the domain, and
throws `ConcurrencyException` on a mismatch. That check has a race window between
the read and the write, so the real guarantee is the `UNIQUE (aggregate_id, version)`
constraint on `account_events` - if two transactions both pass the version check and
try to insert the same version, the database rejects the second insert and it gets
translated back into the same `ConcurrencyException`.

## Why event sourcing instead of just updating a balance column

Updating a balance in place is simpler and is the right call for most applications.
It's worth the extra complexity here because:

- The audit trail isn't a separate feature - every state change is already a
  persisted, immutable record, which is exactly what a compliance audit asks for.
- Point-in-time balance is a query, not a special case - replay up to any event
  version instead of maintaining a separate history table that can drift from the
  "real" balance.
- Concurrency conflicts become explicit and visible (a version mismatch) instead of
  a silent lost update from a `SELECT` followed by an `UPDATE`.

The cost is real: more storage per account, reads that require replay instead of a
single row lookup (mitigated by snapshots, not eliminated), and no built-in way to
"just fix" a bad event the way you'd patch a bad column - correcting a mistake means
appending a compensating event, not editing history.

## Stack

Kotlin 1.9 · Spring Boot 3.2 · PostgreSQL 16 · Flyway · Testcontainers · Docker Compose

## Running it

    docker compose -f docker/docker-compose.yml up -d
    ./gradlew bootRun

    # open an account
    curl -X POST http://localhost:8080/api/accounts \
      -H "Content-Type: application/json" \
      -d '{"ownerId": "user-1", "initialBalance": {"amount": 1000.00, "currency": "BRL"}}'

    # deposit
    curl -X POST http://localhost:8080/api/accounts/{id}/deposit \
      -H "Content-Type: application/json" \
      -d '{"amount": {"amount": 250.00, "currency": "BRL"}}'

    # withdraw
    curl -X POST http://localhost:8080/api/accounts/{id}/withdraw \
      -H "Content-Type: application/json" \
      -d '{"amount": {"amount": 100.00, "currency": "BRL"}}'

    # transfer
    curl -X POST http://localhost:8080/api/accounts/{id}/transfer \
      -H "Content-Type: application/json" \
      -d '{"toAccountId": "<other-id>", "amount": {"amount": 50.00, "currency": "BRL"}}'

    # check balance
    curl http://localhost:8080/api/accounts/{id}/balance

    # full event history
    curl http://localhost:8080/api/accounts/{id}/history

## Tests

    ./gradlew test

Covers `Account` replay (including replay from a snapshot), the command services
against mocked ports (version passed to `append`, snapshot cadence, concurrency
propagation), and the query services.

    ./gradlew integrationTest

Runs the Testcontainers suite against a real Postgres container: deposits/withdrawals
through the actual `PostgresEventStore`/`PostgresSnapshotStore`, the snapshot row
actually landing at version 50, and a stale-version append actually throwing
`ConcurrencyException` against the database's unique constraint. I kept this in a
separate Gradle task instead of folding it into `test` because it needs Docker
running locally, and I didn't want `./gradlew build` to fail on a machine or CI
runner where Docker isn't available - CI runs both tasks.

## Known limitations

- Transfers are not atomic across the two accounts involved: the source event is
  appended, then the destination event. If the process crashes between the two
  appends, the source is debited without a matching credit. A real system would
  need a saga/process manager (or an outbox + a reconciliation job) to close that
  gap - this project doesn't have one.
- The command and query sides share one Postgres instance and one schema. CQRS here
  is a code-level split, not an infrastructure one.
- The snapshot interval (50 events) is a hardcoded constant in
  `AccountCommandService`, not configurable per account or via `application.yml`.
- No event schema versioning. If a field on `MoneyDeposited` needs to change shape,
  existing rows in `account_events` would need a migration or an upcasting step -
  neither exists yet.
- No outbox/publishing step. Events are appended and read back from the same table;
  nothing publishes them to a broker for other services to consume.
- `EventSerializer` maps event type names to classes with a fixed map - renaming an
  event class breaks deserialization of every event already persisted under the old
  name.
