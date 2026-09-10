package com.transit.reliability.service;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.transit.reliability.config.KafkaTopicProperties;
import com.transit.reliability.model.TripUpdateEvent;
import com.transit.reliability.parser.MtaGtfsRealtimeMapper;
import com.transit.reliability.parser.MtaGtfsRealtimeParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.SendResult;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MtaRealtimePollerTest {

    private HttpServer server;
    private int port;
    private AtomicInteger requestCount;
    private AtomicReference<String> receivedApiKey;

    private byte[] responseBytes;
    private int responseStatus;
    private String responseContentType;

    private MtaGtfsRealtimeParser parser;
    private MtaGtfsRealtimeMapper mapper;
    private TestTripUpdateProducer producer;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() throws IOException {
        requestCount = new AtomicInteger(0);
        receivedApiKey = new AtomicReference<>();

        responseStatus = 200;
        responseBytes = new byte[0];
        responseContentType = null;

        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();

        server.createContext("/feed", exchange -> {
            requestCount.incrementAndGet();

            List<String> keys =
                    exchange.getRequestHeaders().get("x-api-key");

            if (keys != null && !keys.isEmpty()) {
                receivedApiKey.set(keys.get(0));
            }

            if (responseContentType != null) {
                exchange.getResponseHeaders()
                        .set("Content-Type", responseContentType);
            }

            exchange.sendResponseHeaders(
                    responseStatus,
                    responseBytes.length
            );

            if (responseBytes.length > 0) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } else {
                exchange.getResponseBody().close();
            }
        });

        server.start();

        parser = new MtaGtfsRealtimeParser();
        mapper = new MtaGtfsRealtimeMapper();
        producer = new TestTripUpdateProducer();

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void testSuccessfulFetchParsePublishFlow() {
        FeedHeader header = FeedHeader.newBuilder()
                .setTimestamp(123456L)
                .setGtfsRealtimeVersion("2.0")
                .build();

        TripUpdate tripUpdate = TripUpdate.newBuilder()
                .setTrip(
                        com.google.transit.realtime.GtfsRealtime.TripDescriptor
                                .newBuilder()
                                .setTripId("trip-1")
                                .setRouteId("A")
                                .build()
                )
                .build();

        FeedEntity entity = FeedEntity.newBuilder()
                .setId("1")
                .setTripUpdate(tripUpdate)
                .build();

        FeedMessage feedMessage = FeedMessage.newBuilder()
                .setHeader(header)
                .addEntity(entity)
                .build();

        responseBytes = feedMessage.toByteArray();

        MtaRealtimePoller poller =
                new MtaRealtimePoller(
                        parser,
                        mapper,
                        producer,
                        feedUrl(),
                        true,
                        httpClient
                );

        poller.poll();

        assertThat(requestCount.get()).isEqualTo(1);
        assertThat(producer.getPublishedEvents()).hasSize(1);

        TripUpdateEvent event =
                producer.getPublishedEvents().get(0);

        assertThat(event.getTripId()).isEqualTo("trip-1");
        assertThat(event.getRouteId()).isEqualTo("A");
    }

    @Test
    void testMalformedNonGtfsResponse() {
        responseBytes =
                "invalid-non-protobuf-data-garbage".getBytes();

        MtaRealtimePoller poller =
                new MtaRealtimePoller(
                        parser,
                        mapper,
                        producer,
                        feedUrl(),
                        true,
                        httpClient
                );

        poller.poll();

        assertThat(requestCount.get()).isEqualTo(1);
        assertThat(producer.getPublishedEvents()).isEmpty();
    }

    @Test
    void testFeedContainingNoTripUpdates() {
        FeedHeader header = FeedHeader.newBuilder()
                .setTimestamp(123456L)
                .setGtfsRealtimeVersion("2.0")
                .build();

        FeedEntity entity = FeedEntity.newBuilder()
                .setId("2")
                .setAlert(
                        com.google.transit.realtime.GtfsRealtime.Alert
                                .newBuilder()
                                .build()
                )
                .build();

        FeedMessage feedMessage = FeedMessage.newBuilder()
                .setHeader(header)
                .addEntity(entity)
                .build();

        responseBytes = feedMessage.toByteArray();

        MtaRealtimePoller poller =
                new MtaRealtimePoller(
                        parser,
                        mapper,
                        producer,
                        feedUrl(),
                        true,
                        httpClient
                );

        poller.poll();

        assertThat(requestCount.get()).isEqualTo(1);
        assertThat(producer.getPublishedEvents()).isEmpty();
    }

    @Test
    void testDisabledPollingDoesNotExecute() {
        MtaRealtimePoller poller =
                new MtaRealtimePoller(
                        parser,
                        mapper,
                        producer,
                        feedUrl(),
                        false,
                        httpClient
                );

        poller.poll();

        assertThat(requestCount.get()).isEqualTo(0);
        assertThat(producer.getPublishedEvents()).isEmpty();
    }

    @Test
    void testXmlResponseHandledGracefully() {
        responseBytes = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Error>
                    <Code>NoSuchBucket</Code>
                    <Message>The specified bucket does not exist</Message>
                </Error>
                """.getBytes();

        responseContentType = "application/xml";

        MtaRealtimePoller poller =
                new MtaRealtimePoller(
                        parser,
                        mapper,
                        producer,
                        feedUrl(),
                        true,
                        httpClient
                );

        poller.poll();

        assertThat(requestCount.get()).isEqualTo(1);
        assertThat(producer.getPublishedEvents()).isEmpty();
    }

    @Test
    void testXmlBodySniffedGracefully() {
        responseBytes =
                "   <Error><Code>NoSuchBucket</Code></Error>".getBytes();

        MtaRealtimePoller poller =
                new MtaRealtimePoller(
                        parser,
                        mapper,
                        producer,
                        feedUrl(),
                        true,
                        httpClient
                );

        poller.poll();

        assertThat(requestCount.get()).isEqualTo(1);
        assertThat(producer.getPublishedEvents()).isEmpty();
    }

    @Test
    void testApiKeyPassedInRequestHeader() {
        FeedHeader header = FeedHeader.newBuilder()
                .setTimestamp(123456L)
                .setGtfsRealtimeVersion("2.0")
                .build();

        TripUpdate tripUpdate = TripUpdate.newBuilder()
                .setTrip(
                        com.google.transit.realtime.GtfsRealtime.TripDescriptor
                                .newBuilder()
                                .setTripId("trip-abc")
                                .setRouteId("C")
                                .build()
                )
                .build();

        FeedEntity entity = FeedEntity.newBuilder()
                .setId("2")
                .setTripUpdate(tripUpdate)
                .build();

        FeedMessage feedMessage = FeedMessage.newBuilder()
                .setHeader(header)
                .addEntity(entity)
                .build();

        responseBytes = feedMessage.toByteArray();

        MtaRealtimePoller poller =
                new MtaRealtimePoller(
                        parser,
                        mapper,
                        producer,
                        feedUrl(),
                        "my-secret-test-api-key",
                        true,
                        httpClient
                );

        poller.poll();

        assertThat(requestCount.get()).isEqualTo(1);
        assertThat(receivedApiKey.get())
                .isEqualTo("my-secret-test-api-key");

        assertThat(producer.getPublishedEvents()).hasSize(1);
        assertThat(
                producer.getPublishedEvents().get(0).getTripId()
        ).isEqualTo("trip-abc");
    }

    private String feedUrl() {
        return "http://localhost:" + port + "/feed";
    }

    static class TestTripUpdateProducer extends TripUpdateProducer {

        private final List<TripUpdateEvent> publishedEvents =
                new ArrayList<>();

        TestTripUpdateProducer() {
            super(
                    null,
                    new KafkaTopicProperties(
                            "trip-timing",
                            "trip-update",
                            "trip-update.DLT",
                            "service-alert"
                    )
            );
        }

        @Override
        public CompletableFuture<SendResult<String, TripUpdateEvent>>
        sendTripUpdateEvent(TripUpdateEvent event) {

            publishedEvents.add(event);
            return CompletableFuture.completedFuture(null);
        }

        List<TripUpdateEvent> getPublishedEvents() {
            return publishedEvents;
        }
    }
}