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
