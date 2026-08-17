# Design Decisions

This document captures the main architecture and engineering decisions for the NYC Transit Reliability & Bottleneck Detector.

The goal is to keep the system small enough to reason about while still supporting meaningful experimentation with event-driven processing, throughput, scalability, database performance, and fault tolerance.

## 1. Processing Model

The system has two primary processing paths.

### Realtime Processing

The realtime path supports rider-facing information such as:

- Current trip delays
- Updated arrival times
- Current route degradation

Freshness is more important than processing every historical event.

### Analytics Processing

The analytics path supports operator-facing questions such as:

- Route health over time
- Recurring delays by stop
- Recurring problems by time of day
- Hourly, daily, and weekly reliability trends

Completeness is more important than immediate freshness.

The two paths should be independently processed and independently scalable so analytics lag or failure does not block realtime rider information.

---

## 2. Ingestion and Business Logic

Ingestion is responsible only for:

- Receiving external transit data
- Validating it
- Normalizing it into internal event schemas
- Publishing normalized events

Business calculations do not belong in ingestion.

The realtime processor is responsible for calculations such as:

- Delay relative to schedule
- Arrival-time changes
- Route degradation
- Anomaly detection

This keeps source-specific normalization separate from transit business rules.

---

## Source Data Mapping

The realtime and static GTFS feeds serve different purposes.

```text
Static GTFS
    |
    | scheduled stop times
    |
    +------------------+
                       |
GTFS-Realtime          |
TripUpdate             |
    |                  |
    | predicted times  |
    +--------+---------+
             |
             v
      Normalized TripUpdateEvent
             |
             v
      Realtime Processor
             |
             v
       DelayObservation
```

The ingestion layer is responsible for converting external GTFS representations into the internal event model.

### Realtime to Static Trip Matching Limitations

When matching NYC subway realtime `TripUpdate` messages to static GTFS schedules, it is important to note that **the MTA does not guarantee exact static-trip matching**.

Due to dynamic operational changes, dispatch decisions, and real-world subway conditions, some realtime trip identifiers (such as custom MTA trip IDs) may not match any trip ID defined in the static GTFS schedule. Consequently, matching algorithms must be resilient to unmatched trips, and the system must handle cases where scheduled static details (like static timetables) are unavailable or cannot be mapped directly.

The realtime processor owns business calculations such as:

```text
delay = predicted time - scheduled time
```

This keeps external-source parsing separate from transit reliability logic.

## 3. Kafka

Kafka is used to provide:

- Decoupling between ingestion and downstream processing
- Independent scaling of realtime and analytics consumers
- Buffering during traffic bursts
- Replay after consumer failure
- Ordering where processing order affects correctness
- Isolation between workloads

### Trip Update Topic

MTA GTFS-Realtime provides a `TripUpdate` as a snapshot of a trip containing timing predictions for multiple upcoming stops.

The normalized Kafka event preserves that trip-level snapshot rather than publishing one Kafka event for every stop.

Partition key:

```text
tripId
```

This ensures successive snapshots for the same trip remain ordered while different trips can be processed in parallel.

Conceptual event:

```json
{
  "tripId": "",
  "routeId": "",
  "transportMode": "BUS | SUBWAY",
  "eventTimestamp": "",
  "stopUpdates": [
    {
      "stopId": "",
      "scheduledArrival": "",
      "predictedArrival": "",
      "scheduledDeparture": "",
      "predictedDeparture": ""
    }
  ]
}
```

The realtime GTFS feed provides predicted timing information.

Scheduled arrival/departure information comes from the static GTFS schedule and may be added during normalization.

The ingestion layer performs source parsing, normalization, and schedule enrichment, but it does not calculate delay or determine whether a condition is anomalous.

Fields such as trip status or completed-stop count should not be added to this event unless they can be obtained reliably from the source data.

### Service Alert Topic

Service alerts have different semantics and schema-evolution needs from trip updates, so they use a separate topic.

Partition key:

```text
alertId
```

This preserves ordering across the lifecycle of the same alert.

Conceptual event:

```json
{
  "alertId": "",
  "alertType": "",
  "startTime": "",
  "endTime": "",
  "status": "IN_PROGRESS | RESOLVED",
  "routeId": "",
  "stopId": "",
  "description": ""
}
```

### Anomaly Topic

The realtime processor may emit significant conditions as anomaly events.

Conceptual event:

```json
{
  "routeId": "",
  "stopId": "",
  "type": "",
  "detectedTime": "",
  "status": "ACTIVE | RESOLVED"
}
```

Anomaly is primarily an internal processing concept rather than a rider-facing domain object.

Analytics should not rely only on anomaly events because normal historical observations are also required to establish baselines and calculate route-health trends.

## 4. Anomaly Definition

The initial anomaly types are:

### Trip Delay Anomaly

A trip's delay exceeds an acceptable threshold.

### Persistent Service Alert

A service alert remains unresolved for an unusually long period.

### Route Degradation

Overall route health deteriorates based on a combination of:

- Increasing delays
- Increased trip duration
- Service-alert activity

Exact thresholds will be configurable and refined through testing rather than hard-coded into the initial design.

---

## 5. Domain Model

The core relational concepts are:

- Route
- Trip
- Vehicle
- Stop
- Schedule
- ServiceAlert

Important relationships include:

- A Route has many Trips.
- A Route contains many Stops.
- A Stop can serve many Routes.
- A Trip represents a current/live instance of a Route.
- A Trip follows a Schedule.
- A Trip is associated with a Vehicle.
- Service alerts may be associated with routes, stops, trips, or vehicles depending on available source information.

Trip status is maintained as part of current trip state rather than as a separate status table.

---

## 6. Persistence Choice

The system uses:

```text
PostgreSQL + TimescaleDB
```

This is intentionally one database platform supporting two different workloads.

### Relational / Current-State Data

PostgreSQL is used for structured operational data such as:

- Route
- Trip
- Vehicle
- Stop
- Schedule
- ServiceAlert
- Current trip delay/status
- Current arrival information
- Current route degradation state

### Historical / Time-Series Data

TimescaleDB capabilities are used for historical observations such as:

- TripUpdate
- DelayObservation
- RouteHealthMetric

Time-series queries are a core requirement from the beginning, so TimescaleDB is included from the start rather than introduced only after PostgreSQL becomes insufficient.

---

## 7. Historical Data Granularity

The source event granularity and analytical granularity are intentionally different.

### TripUpdate

One record represents one normalized source snapshot for a trip at a point in time.

A TripUpdate may contain predicted timing information for multiple upcoming stops.

This preserves the coherent state received from the transit source without multiplying each source update into many Kafka messages.

### DelayObservation

The realtime processor derives granular delay observations from a TripUpdate.

One DelayObservation represents:

```text
one trip
+ one stop
+ one observation time
+ one calculated delay
```

Examples include:

- Scheduled vs predicted arrival difference
- Scheduled vs predicted departure difference
- Other stop-level timing deviations that can be derived reliably

This stop-level granularity supports:

- Recurring delay analysis by stop
- Route-health calculations
- Time-of-day delay patterns
- Historical reliability analysis

### RouteHealthMetric

One record represents a route-health snapshot for a time bucket.

It may include frequently used metrics such as:

- Number of delayed trips
- Aggregate delay
- Trip-duration degradation
- Active service-alert count

The exact bucket size and metric composition will be refined through implementation and performance testing.

## 8. Consistency Requirements

Different data has different freshness and consistency expectations.

### Strong / Current

The following should reflect the latest valid state:

- Rider current trip delay
- Rider changed arrival time
- Current route degradation status

Older events must not overwrite newer rider-facing state.

### Eventually Consistent

The following may lag behind realtime processing:

- Hourly route-health metrics
- Daily analytics
- Weekly analytics

These workloads prioritize completeness over immediate freshness.

This is a business-level consistency decision and does not imply that the strongest database transaction-isolation level must always be used.

---

## 9. Duplicate and Out-of-Order Events

### Duplicate TripUpdate

If the same event is received more than once:

- Processing should be idempotent.
- Duplicate state/history should not be created.

### Out-of-Order TripUpdate

If an older event arrives after a newer event for the same trip:

- It must not replace newer rider-facing state.
- It may still be retained for historical analytics when appropriate.
- Historical calculations should respect event time.

Kafka ordering by `tripId` reduces this problem but does not eliminate the need to handle late events.

---

## 10. Consumer Failure and Replay

A Kafka consumer can:

1. Process an event.
2. Successfully write its result to the database.
3. Crash before committing the Kafka offset.

The event may therefore be delivered again.

Database writes and downstream processing must be idempotent so redelivery does not corrupt state or create duplicate history.

---

## 11. Retry and Dead-Letter Handling

Processing failures use bounded retries.

If an event repeatedly fails:

```text
event
  ↓
bounded retries
  ↓
retry exhausted
  ↓
DLQ
```

A poison event must not permanently block unrelated trip processing.

---

## 12. Transactional Outbox

Some processing may require both:

1. Updating database state
2. Publishing a downstream event such as an anomaly

If those operations must behave atomically, a transactional outbox is a candidate.

This protects against:

```text
DB update succeeds
        ↓
process crashes
        ↓
Kafka event never published
```

The outbox should only be introduced where this consistency requirement actually exists.

---

## 13. Overload Strategy

### Realtime Path

Realtime processing prioritizes freshness.

During a traffic spike:

1. Scale processing capacity where possible.
2. Buffer events for a bounded period.
3. Reprocess recent events after recovery.
4. Stop processing events that have become too stale to be useful to riders.

The system should preserve an indication that realtime data was unavailable or incomplete during the affected period.

### Analytics Path

Analytics prioritizes completeness.

During overload:

- Preserve the backlog.
- Allow temporary lag.
- Eventually process all relevant historical events.

A temporary analytics delay of roughly 30 minutes is acceptable for the initial system.

---

## 14. Database Slowdown

A slow database should not automatically stop every processing path.

For data that riders directly query from persisted state:

- Processing may need to apply backpressure so persisted state does not fall significantly behind processed state.

For processing that does not require synchronous rider-visible persistence:

- Work may continue or buffer independently.

Analytics processing and analytics writes should not block the realtime rider path.

---

## 15. External Feed Failure

If the primary realtime MTA feed becomes unavailable:

- An alternative MTA source may be used when one is available and compatible.
- Otherwise, the system should expose that current information may be stale or unavailable.
- The missing-data period should be represented so users and analytics do not incorrectly assume complete observations.

---

## 16. Traffic Correlation

NYC road-traffic observations may be incorporated for bus routes.

Conceptually:

```text
Bus delay observations
          +
Road traffic observations
          ↓
Transit degradation context
```

Traffic information may help determine whether bus degradation occurs at the same time as poor road conditions.

This is correlation, not proof of causation.

Road-traffic correlation does not apply to subway routes.

Traffic data should use its own normalized event stream rather than changing the existing trip/timing or service-alert schemas.

Historical traffic observations are natural candidates for TimescaleDB storage.

---

## 17. Scalability and Fault-Tolerance Experiments

The architecture should support deliberate experiments rather than relying only on theoretical scalability claims.

### Traffic Spike

Increase event volume several times above the normal baseline.

Measure:

- Realtime latency
- Kafka consumer lag
- Analytics lag
- Database write latency
- Processing throughput
- Recovery after traffic returns to normal

### Realtime Consumer Failure

Stop the realtime consumer and restart it later.

Verify:

- Recent useful events are replayed.
- Stale rider events are skipped.
- Current state does not move backward.
- Missing-data periods are represented.
- Recovery time can be measured.

### Slow Database

Artificially increase persistence latency.

Measure:

- Backpressure
- Consumer lag
- Rider-data freshness
- Analytics isolation
- Recovery after database performance returns to normal

### Poison Event

Inject an event that repeatedly fails.

Verify:

- Bounded retries occur.
- The event reaches the DLQ.
- Unrelated processing continues.

### Analytics Lag

Artificially slow analytics while realtime remains healthy.

Verify:

- Realtime processing remains unaffected.
- Analytics retains its backlog.
- Analytics eventually catches up completely.

---

## 18. Deferred Decisions

The following should not be added until there is a concrete need.

### Cache

Redis or another cache may be considered later if measurements show current-state database reads becoming a bottleneck.

### Authentication and Authorization

Role-based access can be introduced later, especially for admin/analytics functionality.

Realtime public transit information can remain more broadly accessible.

### Cold Storage

Cold storage is not required initially.

It would become useful if large amounts of old raw historical data need to be retained but are rarely queried.

Possible reasons include:

- Reducing primary database growth
- Lower-cost long-term retention
- Reducing backup/storage burden
- Retaining history for occasional reprocessing or long-term analysis

It should only be introduced if TimescaleDB retention becomes a real operational concern.

### Complaint Data

Rider complaint data may eventually be useful as another historical signal for comparing:

```text
operational degradation
        +
traffic conditions
        +
rider-reported problems
```

It is not required for the initial transit processing system.

---

## 19. Design Principle

The architecture should evolve from measured problems rather than anticipated complexity.

The initial system should therefore favor:

- A small number of clearly defined processing stages
- One relational/time-series database platform
- A small number of Kafka topics
- Independent realtime and analytics processing
- Explicit ordering and idempotency
- Observable failure and recovery behavior

Additional stores, caches, services, or infrastructure should be introduced only when testing demonstrates why they are needed.