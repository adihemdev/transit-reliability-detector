package com.transit.reliability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transit.reliability.model.StopTimingUpdate;
import com.transit.reliability.model.TransportMode;
import com.transit.reliability.model.TripState;
import com.transit.reliability.model.TripStopPrediction;
import com.transit.reliability.model.TripUpdateEvent;
import com.transit.reliability.repository.TripStateRepository;
import com.transit.reliability.service.TripUpdateConsumer;
import com.transit.reliability.service.TripUpdateProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = { "trip-update" })
public class TripUpdateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TripUpdateProducer producer;

    @Autowired
    private TripUpdateConsumer consumer;

    @Autowired
    private TripStateRepository tripStateRepository;

    @BeforeEach
    public void setUp() {
        consumer.clearReceivedEvents();
        tripStateRepository.deleteAll();
    }

    @Test
    public void testTripUpdateEventKafkaPipeline() throws Exception {
        // Given
        String tripId = "mta-trip-999";
        String routeId = "L";
        Instant now = Instant.now();

        List<StopTimingUpdate> stopUpdates = List.of(
                new StopTimingUpdate("L01", null, now.plusSeconds(30), null, now.plusSeconds(40)),
                new StopTimingUpdate("L02", null, now.plusSeconds(120), null, now.plusSeconds(130))
        );

        TripUpdateEvent event = new TripUpdateEvent(
                tripId,
                routeId,
                TransportMode.SUBWAY,
                now,
                stopUpdates
        );

        // When
        producer.sendTripUpdateEvent(event).get();

        // Then: wait and verify that consumer received the event
        waitForEventConsumed();

        List<TripUpdateEvent> received = consumer.getReceivedEvents();
        assertThat(received).hasSize(1);

        TripUpdateEvent receivedEvent = received.get(0);
        assertThat(receivedEvent.getTripId()).isEqualTo(tripId);
        assertThat(receivedEvent.getRouteId()).isEqualTo(routeId);
        assertThat(receivedEvent.getTransportMode()).isEqualTo(TransportMode.SUBWAY);
        assertThat(receivedEvent.getEventTimestamp()).isEqualTo(now);
        assertThat(receivedEvent.getStopUpdates()).hasSize(2);
        assertThat(receivedEvent.getStopUpdates().get(0).getStopId()).isEqualTo("L01");
        assertThat(receivedEvent.getStopUpdates().get(1).getStopId()).isEqualTo("L02");
    }

    @Test
    public void testPersistTripStateAndMultiplePredictions() throws Exception {
        // Given
        String tripId = "trip-persist-123";
        Instant timestamp = Instant.now();
        List<StopTimingUpdate> stopUpdates = List.of(
                new StopTimingUpdate("S01", null, timestamp.plusSeconds(60), null, timestamp.plusSeconds(90)),
                new StopTimingUpdate("S02", null, timestamp.plusSeconds(180), null, timestamp.plusSeconds(210)),
                new StopTimingUpdate("S03", null, timestamp.plusSeconds(300), null, timestamp.plusSeconds(330))
        );
        TripUpdateEvent event = new TripUpdateEvent(tripId, "A", TransportMode.SUBWAY, timestamp, stopUpdates);

        // When
        producer.sendTripUpdateEvent(event).get();
        waitForPersistence(() -> tripStateRepository.existsById(tripId));

        // Then
        Optional<TripState> stateOpt = tripStateRepository.findById(tripId);
        assertThat(stateOpt).isPresent();
        TripState state = stateOpt.get();
        assertThat(state.getRouteId()).isEqualTo("A");
        assertThat(state.getTransportMode()).isEqualTo(TransportMode.SUBWAY);
        assertThat(state.getLastUpdated()).isEqualTo(timestamp);
        
        List<TripStopPrediction> predictions = state.getStopPredictions();
        assertThat(predictions).hasSize(3);
        
        // Assert predictions are ordered or match composite key sequences correctly
        TripStopPrediction p1 = predictions.stream().filter(p -> p.getStopSequence() == 1).findFirst().orElseThrow();
        assertThat(p1.getStopId()).isEqualTo("S01");
        assertThat(p1.getPredictedArrival()).isEqualTo(timestamp.plusSeconds(60));
        assertThat(p1.getPredictedDeparture()).isEqualTo(timestamp.plusSeconds(90));

        TripStopPrediction p2 = predictions.stream().filter(p -> p.getStopSequence() == 2).findFirst().orElseThrow();
        assertThat(p2.getStopId()).isEqualTo("S02");

        TripStopPrediction p3 = predictions.stream().filter(p -> p.getStopSequence() == 3).findFirst().orElseThrow();
        assertThat(p3.getStopId()).isEqualTo("S03");
    }

    @Test
    public void testNewerSnapshotReplacesOldSnapshot() throws Exception {
        // Given
        String tripId = "trip-snapshot-replace";
        Instant initialTime = Instant.now().minusSeconds(100);
        List<StopTimingUpdate> initialStops = List.of(
                new StopTimingUpdate("S01", null, initialTime.plusSeconds(60), null, initialTime.plusSeconds(90)),
                new StopTimingUpdate("S02", null, initialTime.plusSeconds(180), null, initialTime.plusSeconds(210))
        );
        TripUpdateEvent initialEvent = new TripUpdateEvent(tripId, "N", TransportMode.SUBWAY, initialTime, initialStops);

        producer.sendTripUpdateEvent(initialEvent).get();
        waitForPersistence(() -> tripStateRepository.existsById(tripId));

        // Verify initial snapshot exists
        TripState initialSaved = tripStateRepository.findById(tripId).orElseThrow();
        assertThat(initialSaved.getStopPredictions()).hasSize(2);

        // When: sending a newer event with only 1 stop
        Instant newerTime = initialTime.plusSeconds(10);
        List<StopTimingUpdate> newerStops = List.of(
                new StopTimingUpdate("S02_new", null, newerTime.plusSeconds(120), null, newerTime.plusSeconds(150))
        );
        TripUpdateEvent newerEvent = new TripUpdateEvent(tripId, "N", TransportMode.SUBWAY, newerTime, newerStops);

        producer.sendTripUpdateEvent(newerEvent).get();
        // Wait for the consumer to handle the 2nd event (by verifying total consumer processed events list count reaches 2)
        waitForPersistence(() -> consumer.getReceivedEvents().size() >= 2);

        // Then: verify the snapshot has been replaced completely
        TripState updatedSaved = tripStateRepository.findById(tripId).orElseThrow();
        assertThat(updatedSaved.getLastUpdated()).isEqualTo(newerTime);
        
        List<TripStopPrediction> predictions = updatedSaved.getStopPredictions();
        assertThat(predictions).hasSize(1);
        assertThat(predictions.get(0).getStopId()).isEqualTo("S02_new");
        assertThat(predictions.get(0).getStopSequence()).isEqualTo(1);
    }

    @Test
    public void testOutOfOrderEventIgnored() throws Exception {
        // Given
        String tripId = "trip-out-of-order";
        Instant newerTime = Instant.now();
        List<StopTimingUpdate> newerStops = List.of(
                new StopTimingUpdate("S02", null, newerTime.plusSeconds(120), null, newerTime.plusSeconds(150))
        );
        TripUpdateEvent newerEvent = new TripUpdateEvent(tripId, "Q", TransportMode.SUBWAY, newerTime, newerStops);

        producer.sendTripUpdateEvent(newerEvent).get();
        waitForPersistence(() -> tripStateRepository.existsById(tripId));

        // When: sending an older event
        Instant olderTime = newerTime.minusSeconds(10);
        List<StopTimingUpdate> olderStops = List.of(
                new StopTimingUpdate("S01", null, olderTime.plusSeconds(60), null, olderTime.plusSeconds(90)),
                new StopTimingUpdate("S02_old", null, olderTime.plusSeconds(180), null, olderTime.plusSeconds(210))
        );
        TripUpdateEvent olderEvent = new TripUpdateEvent(tripId, "Q", TransportMode.SUBWAY, olderTime, olderStops);

        producer.sendTripUpdateEvent(olderEvent).get();
        // Wait for the consumer to receive both events
        waitForPersistence(() -> consumer.getReceivedEvents().size() >= 2);

        // Then: verify state is NOT overwritten by the older event
        TripState finalSaved = tripStateRepository.findById(tripId).orElseThrow();
        assertThat(finalSaved.getLastUpdated()).isEqualTo(newerTime);
        
        List<TripStopPrediction> predictions = finalSaved.getStopPredictions();
        assertThat(predictions).hasSize(1);
        assertThat(predictions.get(0).getStopId()).isEqualTo("S02");
    }

    @Test
    public void testReprocessingSameEventIsIdempotent() throws Exception {
        // Given
        String tripId = "trip-reprocessing-idempotent";
        Instant time = Instant.now();
        List<StopTimingUpdate> stops = List.of(
                new StopTimingUpdate("S01", null, time.plusSeconds(60), null, time.plusSeconds(90)),
                new StopTimingUpdate("S02", null, time.plusSeconds(180), null, time.plusSeconds(210))
        );
        TripUpdateEvent event = new TripUpdateEvent(tripId, "F", TransportMode.SUBWAY, time, stops);

        producer.sendTripUpdateEvent(event).get();
        waitForPersistence(() -> tripStateRepository.existsById(tripId));

        TripState initialSaved = tripStateRepository.findById(tripId).orElseThrow();
        assertThat(initialSaved.getStopPredictions()).hasSize(2);

        // When: publishing the same event again
        producer.sendTripUpdateEvent(event).get();
        waitForPersistence(() -> consumer.getReceivedEvents().size() >= 2);

        // Then: verify predictions count is still exactly 2 (no duplicates)
        TripState finalSaved = tripStateRepository.findById(tripId).orElseThrow();
        assertThat(finalSaved.getStopPredictions()).hasSize(2);
    }

    @Test
    public void testGetTripStateEndpointWithPredictions() throws Exception {
        // Given
        String tripId = "trip-api-test-predictions";
        Instant time = Instant.now();
        List<StopTimingUpdate> stops = List.of(
                new StopTimingUpdate("S01", null, time.plusSeconds(60), null, time.plusSeconds(90)),
                new StopTimingUpdate("S02", null, time.plusSeconds(180), null, time.plusSeconds(210))
        );
        TripUpdateEvent event = new TripUpdateEvent(tripId, "L", TransportMode.SUBWAY, time, stops);

        producer.sendTripUpdateEvent(event).get();
        waitForPersistence(() -> tripStateRepository.existsById(tripId));

        // When & Then
        mockMvc.perform(get("/api/trips/" + tripId))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String json = result.getResponse().getContentAsString();
                    TripState retrievedState = objectMapper.readValue(json, TripState.class);
                    assertThat(retrievedState.getTripId()).isEqualTo(tripId);
                    assertThat(retrievedState.getRouteId()).isEqualTo("L");
                    
                    List<TripStopPrediction> predictions = retrievedState.getStopPredictions();
                    assertThat(predictions).hasSize(2);
                    assertThat(predictions.get(0).getStopId()).isEqualTo("S01");
                    assertThat(predictions.get(0).getStopSequence()).isEqualTo(1);
                    assertThat(predictions.get(1).getStopId()).isEqualTo("S02");
                    assertThat(predictions.get(1).getStopSequence()).isEqualTo(2);
                });
    }

    private void waitForEventConsumed() throws InterruptedException {
        int retries = 50;
        while (retries-- > 0) {
            if (!consumer.getReceivedEvents().isEmpty()) {
                return;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("TripUpdateEvent was not consumed by the consumer in time.");
    }

    private void waitForPersistence(java.util.function.BooleanSupplier condition) throws InterruptedException {
        int retries = 50;
        while (retries-- > 0) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("Condition not met after waiting.");
    }
}
