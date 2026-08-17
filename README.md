# NYC Transit Reliability & Bottleneck Detector

An event-driven platform for monitoring NYC bus and subway reliability using real-time transit data.

The system processes live transit updates to provide riders with current trip information while helping transit operators analyze route reliability, recurring delays, and service degradation over time.

## What It Does

### Rider View

- View current trip delays
- See changes to expected arrival times
- View relevant service disruptions

### Transit Operations / Analytics

- Identify currently degraded routes
- Track route health over time
- Find recurring delay patterns by stop and time of day
- Compare hourly, daily, and weekly reliability trends

## Data Sources

The project uses NYC MTA GTFS and GTFS-Realtime data, including:

- Trip updates
- Scheduled and predicted arrival/departure times
- Vehicle and trip progress
- Service alerts
- Route, stop, and schedule information

For bus routes, NYC road-traffic observations may also be correlated with transit delays to provide additional context around congestion.

## High-Level Architecture

```text
NYC MTA GTFS / GTFS-Realtime
            |
            v
   Ingestion & Normalization
            |
            v
          Kafka
       /          \
      v            v
Realtime        Analytics
Processing      Processing
      \            /
       v          v
 PostgreSQL + TimescaleDB
            |
            v
         REST API
       /          \
      v            v
 Rider View    Admin View
```

Realtime processing prioritizes fresh trip and arrival information.

Analytics processing maintains historical observations used to understand route and stop reliability over time.

## Technology

- Java
- Spring Boot
- Apache Kafka
- PostgreSQL
- TimescaleDB
- GTFS / GTFS-Realtime

## Project Goals

This project is designed to explore practical distributed-system concerns including:

- Event-driven processing
- Kafka ordering and partitioning
- Consumer scaling
- High-throughput event ingestion
- Idempotent processing
- Retry and failure recovery
- Backpressure
- Time-series data modeling
- Database performance
- Realtime vs eventually consistent workloads
- Fault tolerance under load

## Running Locally

Local setup and run instructions will be added with the first working vertical slice.

The initial runnable flow will be:

```text
MTA Trip Update
      |
      v
Normalize
      |
      v
Kafka
      |
      v
Realtime Processor
      |
      v
Calculate Delay / Arrival Change
      |
      v
PostgreSQL / TimescaleDB
      |
      v
REST API
```

## Documentation

Detailed architecture and engineering tradeoffs are documented separately:

- [`DESIGN_DECISIONS.md`](DESIGN_DECISIONS.md) - Kafka design, persistence choices, consistency, fault tolerance, scalability, and architectural tradeoffs

## Status

Initial architecture and domain design are complete.

Implementation begins with the realtime trip-update vertical slice.
