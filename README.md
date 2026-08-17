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

To run the system locally, follow these steps to spin up the local infrastructure and start the Spring Boot application.

### Prerequisites

- Java 21 or higher
- Maven 3.8+
- Docker & Docker Compose

### 1. Start the Infrastructure

Use Docker Compose to launch PostgreSQL (with TimescaleDB) and Apache Kafka:

```bash
docker compose up -d
```

This starts:
- **Kafka** on port `29092` (using KRaft mode, no Zookeeper required)
- **PostgreSQL / TimescaleDB** on port `5432` with database `transit_db`, username `transit_user`, and password `transit_password`.

The application will automatically enable the `timescaledb` extension and configure the `trip_updates` hypertable partition on startup.

### 2. Build and Run the Application

Compile the code and run the integration tests:

```bash
mvn clean test
```

Start the Spring Boot application:

```bash
mvn spring-boot:run
```

The server starts on port `8080`.

### 3. Verify the End-to-End Flow

You can submit an ingestion event and query the current-state using `curl`.

#### Submit a Trip Timing Event (REST Ingestion -> Kafka Producer)

Send a JSON payload to the `/api/trips/timing` endpoint. This simulates receiving a normalized trip timing update:

```bash
curl -X POST http://localhost:8080/api/trips/timing \
  -H "Content-Type: application/json" \
  -d '{
    "tripId": "trip-mta-101",
    "routeId": "SUBWAY-N",
    "stopId": "stop-ASTORIA-DITMARS",
    "transportMode": "SUBWAY",
    "scheduledArrival": "2026-08-17T12:00:00Z",
    "predictedArrival": "2026-08-17T12:05:00Z",
    "scheduledDeparture": "2026-08-17T12:02:00Z",
    "predictedDeparture": "2026-08-17T12:06:00Z",
    "tripStatus": "IN_TRANSIT",
    "completedStops": 3,
    "eventTimestamp": "2026-08-17T11:55:00Z"
  }'
```

#### Query Latest Trip State (PostgreSQL REST API Query)

Retrieve the latest processed state of the trip. The Kafka consumer processes the event and persists it in PostgreSQL (and the historical update in the TimescaleDB hypertable):

```bash
curl http://localhost:8080/api/trips/trip-mta-101
```

Response:
```json
{
  "tripId": "trip-mta-101",
  "routeId": "SUBWAY-N",
  "stopId": "stop-ASTORIA-DITMARS",
  "transportMode": "SUBWAY",
  "scheduledArrival": "2026-08-17T12:00:00Z",
  "predictedArrival": "2026-08-17T12:05:00Z",
  "scheduledDeparture": "2026-08-17T12:02:00Z",
  "predictedDeparture": "2026-08-17T12:06:00Z",
  "tripStatus": "IN_TRANSIT",
  "completedStops": 3,
  "lastUpdated": "2026-08-17T11:55:00Z"
}
```

---

## Documentation

Detailed architecture and engineering tradeoffs are documented separately:

- [`DESIGN_DECISIONS.md`](DESIGN_DECISIONS.md) - Kafka design, persistence choices, consistency, fault tolerance, scalability, and architectural tradeoffs

## Status

Initial architecture and domain design are complete.

Implementation begins with the realtime trip-update vertical slice.
