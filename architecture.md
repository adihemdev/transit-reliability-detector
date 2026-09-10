# Transit Reliability Platform Architecture

## 1. Overview

This project processes MTA real-time transit data using Spring Boot and Kafka. The system separates ingestion, normalization, event streaming, downstream processing, and persistence so that each stage can scale and fail independently.

The project currently has two event paths:

1. **Trip updates** — the primary real-time transit pipeline.
2. **Service alerts** — a separate event stream for operational alerts and downstream actions.

AWS Lambda has been introduced as a downstream service-alert processor. Direct Lambda invocation is currently a temporary integration step. The planned permanent architecture introduces SQS between the Kafka consumer and Lambda.

---

## 2. Trip Update Pipeline

The existing trip-update flow is:

```text
MTA GTFS-Realtime Trip Update Feed
        |
        v
MtaRealtimePoller
        |
        v
MtaGtfsRealtimeParser
        |
        v
MtaGtfsRealtimeMapper
        |
        v
TripUpdateEvent
        |
        v
TripUpdateProducer
        |
        v
Kafka Trip Update Topic
        |
        v
Trip Update Consumer
        |
        v
PostgreSQL / TimescaleDB
```

### Responsibilities

- **MtaRealtimePoller** fetches the GTFS-Realtime feed on a schedule.
- **MtaGtfsRealtimeParser** parses the protobuf feed.
- **MtaGtfsRealtimeMapper** converts MTA/GTFS data into the application's normalized `TripUpdateEvent` model.
- **TripUpdateProducer** publishes normalized events to Kafka.
- **Kafka** acts as the durable event-stream backbone.
- **Consumers** process the stream independently and persist the resulting state/history.

### Why Kafka is used

Kafka is appropriate for the core transit event stream because it provides:

- durable event retention;
- high-throughput event processing;
- partition-based horizontal scalability;
- ordering within a partition;
- consumer groups for independent/scalable processing;
- offset-based recovery and replay.

The project will further validate this architecture through producer batching/compression, consumer replay, retries, DLQ handling, idempotency, partition distribution, consumer lag, and load testing.

---

## 3. Service Alert Ingestion

Service alerts are intentionally kept separate from trip updates because they represent a different event type and can trigger different downstream behavior.

The ingestion path is:

```text
MTA Service Alert Feed
        |
        v
MtaServiceAlertPoller
        |
        v
ServiceAlertPublisher
        |
        v
MtaServiceAlertFeedParser
        |
        v
ServiceAlertEvent
        |
        v
ServiceAlertProducer
        |
        v
Kafka Service Alert Topic
```

### Responsibilities

- **MtaServiceAlertPoller** fetches the MTA service-alert feed.
- **ServiceAlertPublisher** coordinates parsing and publication.
- **MtaServiceAlertFeedParser** converts the source feed into normalized `ServiceAlertEvent` objects.
- **ServiceAlertProducer** publishes each alert to Kafka using `alertId` as the Kafka message key.

Using `alertId` as the key provides a stable business identifier and gives Kafka a deterministic key for partitioning related events.

---

## 4. Temporary Direct-Lambda Path

A Java 21 AWS Lambda named `mta-service-alert-processor` has been created and successfully invoked from the local Spring Boot application.

The temporary integration path is:

```text
Kafka Service Alert Topic
        |
        v
ServiceAlertConsumer
        |
        v
ServiceAlertLambdaInvoker
        |
        v
AWS Lambda
```

`ServiceAlertLambdaInvoker` uses the AWS SDK for Java to invoke the Lambda function. The local application authenticates to AWS using temporary credentials rather than hard-coded AWS access keys.

The direct invocation path is useful for validating:

- Java-to-Lambda invocation;
- request serialization;
- IAM authentication/authorization;
- Lambda handler configuration;
- CloudWatch logging;
- cold-start versus warm execution behavior;
- application handling of Lambda invocation failures.

### Important limitation

Direct invocation couples the Kafka consumer to Lambda execution. If Lambda or a downstream dependency is slow, the Kafka consumer must wait. If the Lambda call fails, that failure occurs directly in the Kafka-consumer processing path.

For that reason, direct invocation is **not the intended permanent architecture**.

---

## 5. Planned Service Alert Action Architecture

The planned production-style downstream path is:

```text
Kafka Service Alert Topic
        |
        v
ServiceAlertConsumer
        |
        v
SQS Service Alert Action Queue
        |
        v
Lambda Service Alert Action Processor
        |
        v
Notification / Incident / Other Downstream System
```

Kafka and SQS have deliberately different responsibilities.

### Kafka responsibility

Kafka remains the durable transit-domain event stream. It stores normalized service-alert events and allows independent consumers to process or replay those events.

### SQS responsibility

SQS represents **units of downstream work** generated from those events.

Once the Kafka consumer successfully hands an alert action to SQS, it does not need to wait for the downstream action to complete.

This provides:

- asynchronous decoupling from downstream processing;
- buffering during bursts;
- failure isolation;
- retry through message redelivery;
- dead-letter queue support for poison messages;
- controlled Lambda concurrency;
- a practical at-least-once-delivery/idempotency model.

### Why not call the downstream system directly from the Kafka consumer?

Without an asynchronous boundary:

```text
Kafka Consumer
      |
      v
External API
      |
      +---- slow / unavailable
                 |
                 v
        Kafka processing slows
```

With SQS:

```text
Kafka Consumer
      |
      v
     SQS
      |
      +---- Kafka consumer completes its handoff

SQS
 |
 v
Lambda
 |
 v
External API
```

A notification-system outage therefore does not need to stop the main Kafka event-processing path. Work can remain buffered in SQS and be retried independently.

---

## 6. Lambda Responsibility

The Lambda should not exist merely to log or echo a Kafka event. Its eventual responsibility is to perform a **short-lived, stateless downstream action** associated with a normalized service alert.

A representative responsibility is service-disruption action processing:

```text
Receive ServiceAlertEvent
        |
        v
Validate event
        |
        v
Determine whether an action is required
        |
        v
Build notification / incident payload
        |
        v
Invoke downstream system
```

Possible downstream actions include:

- sending an operational notification;
- creating or updating an incident;
- publishing to an external alerting system;
- triggering another bounded service-alert workflow.

Lambda is a reasonable fit because this work is:

- event-driven;
- stateless;
- short-lived;
- independently scalable;
- potentially bursty during major service disruptions.

A large disruption could produce a sudden increase in alert work. SQS can buffer the burst and Lambda can scale processing independently of the Kafka consumer.

If the project ultimately has no meaningful downstream action of this type, SQS and Lambda should be removed rather than retained without a clear architectural purpose.

---

## 7. Failure and Delivery Semantics

The design intentionally exposes different reliability mechanisms at different layers.

### Kafka

The Kafka work will cover:

- producer acknowledgement and durability settings;
- batching and compression;
- consumer offset management;
- at-least-once processing;
- consumer crash and replay behavior;
- retry and dead-letter handling;
- idempotent database processing;
- partition-key distribution and hot partitions;
- consumer lag under downstream slowdown.

### SQS and Lambda

The SQS/Lambda path will later cover:

- SQS Standard at-least-once delivery;
- visibility timeout;
- Lambda SQS event source mappings;
- batch size and batching window;
- maximum Lambda concurrency;
- retry/redelivery behavior;
- dead-letter queue redrive policy;
- duplicate delivery and Lambda idempotency;
- downstream-failure isolation.

These mechanisms are complementary rather than interchangeable: Kafka manages the durable event stream, while SQS manages asynchronous downstream work execution.

---

## 8. Configuration

Grouped application settings should use Spring Boot `@ConfigurationProperties` rather than scattered `@Value` fields.

Current configuration groups include:

```text
MtaServiceAlertProperties
    feedUrl
    apiKey
    enabled
    pollingInterval

AwsProperties
    region
    lambda
        serviceAlertFunctionName
```

AWS credentials must not be stored in application configuration or source control. Local development uses temporary AWS credentials resolved through the AWS SDK credential provider chain.

---

## 9. Current Status

### Implemented

- MTA trip-update polling and normalization.
- Kafka trip-update publication and consumption.
- Database-backed trip-update processing.
- Service-alert feed parsing and normalization.
- Kafka `ServiceAlertEvent` producer path.
- Java 21 AWS Lambda deployment.
- Custom `ServiceAlertHandler` invocation.
- CloudWatch Lambda logging.
- Cold-start and warm-invocation observation.
- Local Java AWS authentication.
- `ServiceAlertLambdaInvoker` with basic failure handling.
- Unit test for the Lambda invoker.
- Real Java-to-AWS-Lambda integration test.
- `ServiceAlertConsumer` for the service-alert Kafka topic.

### Verification deferred

The full service-alert path still needs to be exercised end-to-end with a real alert:

```text
MTA Service Alert Feed
    -> Spring Boot
    -> Kafka
    -> ServiceAlertConsumer
    -> real AWS Lambda
```

### Planned

The direct Lambda invocation will later be replaced by:

```text
ServiceAlertConsumer
    -> SQS
    -> Lambda
```

with visibility timeout, retries, DLQ handling, idempotency, and concurrency controls added and tested.

---

## 10. Near-Term Implementation Sequence

The current priority is to return to the Kafka fundamentals before adding more AWS infrastructure:

```text
1. Kafka producer batching and compression
2. Consumer failure / offset replay
3. Kafka retry and DLQ handling
4. Idempotent event processing
5. Verify the existing service-alert Kafka -> Lambda path end-to-end
6. Introduce SQS between ServiceAlertConsumer and Lambda
7. Exercise SQS visibility timeout, retry, DLQ, duplicate delivery, and concurrency
8. Continue stale/out-of-order event handling
9. Prediction-slippage logic
10. TimescaleDB/history analysis
11. Route-health calculations
12. Final load and failure testing
```

This ordering keeps the Kafka event-stream design solid before introducing the second delivery model provided by SQS/Lambda.
