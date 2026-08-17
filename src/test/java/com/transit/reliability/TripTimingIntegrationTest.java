package com.transit.reliability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transit.reliability.model.TransportMode;
import com.transit.reliability.model.TripState;
import com.transit.reliability.model.TripStatus;
import com.transit.reliability.model.TripTimingEvent;
import com.transit.reliability.model.TripUpdate;
import com.transit.reliability.repository.TripStateRepository;
import com.transit.reliability.repository.TripUpdateRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = { "trip-timing" })
public class TripTimingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TripStateRepository tripStateRepository;

    @Autowired
    private TripUpdateRepository tripUpdateRepository;

    @BeforeEach
    public void setUp() {
        tripStateRepository.deleteAll();
        tripUpdateRepository.deleteAll();
    }

    @Test
    public void testEndToEndTripTimingFlow() throws Exception {
        String tripId = "trip-101";
        Instant initialTimestamp = Instant.now().minus(10, ChronoUnit.MINUTES);

        // 1. Submit initial trip timing event
        TripTimingEvent initialEvent = new TripTimingEvent(
                tripId,
                "route-A",
                "stop-12",
                TransportMode.SUBWAY,
                initialTimestamp.plus(5, ChronoUnit.MINUTES),
                initialTimestamp.plus(7, ChronoUnit.MINUTES),
                initialTimestamp.plus(6, ChronoUnit.MINUTES),
                initialTimestamp.plus(8, ChronoUnit.MINUTES),
                TripStatus.IN_TRANSIT,
                2,
                initialTimestamp
        );

        mockMvc.perform(post("/api/trips/timing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initialEvent)))
                .andExpect(status().isAccepted());

        // Wait for Kafka to consume and persist
        waitForPersistence(() -> tripStateRepository.existsById(tripId));

        // Verify TripState (current state) in DB
        Optional<TripState> stateOpt = tripStateRepository.findById(tripId);
        assertThat(stateOpt).isPresent();
        TripState currentState = stateOpt.get();
        assertThat(currentState.getTripId()).isEqualTo(tripId);
        assertThat(currentState.getTripStatus()).isEqualTo(TripStatus.IN_TRANSIT);
        assertThat(currentState.getLastUpdated()).isEqualTo(initialTimestamp);

        // Verify TripUpdate (historical/timeseries) in DB
        List<TripUpdate> updates = tripUpdateRepository.findAll();
        assertThat(updates).hasSize(1);
        assertThat(updates.get(0).getTripId()).isEqualTo(tripId);
        assertThat(updates.get(0).getEventTimestamp()).isEqualTo(initialTimestamp);

        // 2. Submit a newer trip event (simulating normal forward progress)
        Instant newerTimestamp = initialTimestamp.plus(2, ChronoUnit.MINUTES);
        TripTimingEvent newerEvent = new TripTimingEvent(
                tripId,
                "route-A",
                "stop-13",
                TransportMode.SUBWAY,
                initialTimestamp.plus(15, ChronoUnit.MINUTES),
                initialTimestamp.plus(16, ChronoUnit.MINUTES),
                initialTimestamp.plus(16, ChronoUnit.MINUTES),
                initialTimestamp.plus(17, ChronoUnit.MINUTES),
                TripStatus.AT_STOP,
                3,
                newerTimestamp
        );

        mockMvc.perform(post("/api/trips/timing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newerEvent)))
                .andExpect(status().isAccepted());

        // Wait for Kafka consumption (we look for 2 updates total)
        waitForPersistence(() -> tripUpdateRepository.count() == 2);

        // Verify TripState has been updated to newer values
        currentState = tripStateRepository.findById(tripId).orElseThrow();
        assertThat(currentState.getTripStatus()).isEqualTo(TripStatus.AT_STOP);
        assertThat(currentState.getStopPredictions()).hasSize(1);
        assertThat(currentState.getStopPredictions().get(0).getStopId()).isEqualTo("stop-13");
        assertThat(currentState.getLastUpdated()).isEqualTo(newerTimestamp);

        // 3. Submit an out-of-order/older trip event
        Instant olderTimestamp = initialTimestamp.minus(5, ChronoUnit.MINUTES);
        TripTimingEvent olderEvent = new TripTimingEvent(
                tripId,
                "route-A",
                "stop-11",
                TransportMode.SUBWAY,
                initialTimestamp.plus(2, ChronoUnit.MINUTES),
                initialTimestamp.plus(2, ChronoUnit.MINUTES),
                initialTimestamp.plus(3, ChronoUnit.MINUTES),
                initialTimestamp.plus(3, ChronoUnit.MINUTES),
                TripStatus.IN_TRANSIT,
                1,
                olderTimestamp
        );

        mockMvc.perform(post("/api/trips/timing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(olderEvent)))
                .andExpect(status().isAccepted());

        // Wait for Kafka consumption (we look for 3 updates total)
        waitForPersistence(() -> tripUpdateRepository.count() == 3);

        // Verify TripState is NOT overwritten (must remain AT_STOP and stop-13 from newerTimestamp)
        currentState = tripStateRepository.findById(tripId).orElseThrow();
        assertThat(currentState.getTripStatus()).isEqualTo(TripStatus.AT_STOP);
        assertThat(currentState.getStopPredictions()).hasSize(1);
        assertThat(currentState.getStopPredictions().get(0).getStopId()).isEqualTo("stop-13");
        assertThat(currentState.getLastUpdated()).isEqualTo(newerTimestamp);

        // 4. Submit a duplicate event to test historical duplicate prevention
        mockMvc.perform(post("/api/trips/timing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newerEvent)))
                .andExpect(status().isAccepted());

        // Sleep briefly to ensure processing has completed (count should remain 3)
        Thread.sleep(1500);
        assertThat(tripUpdateRepository.count()).isEqualTo(3);

        // 5. Query latest trip state via REST GET endpoint
        mockMvc.perform(get("/api/trips/" + tripId))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String json = result.getResponse().getContentAsString();
                    TripState retrievedState = objectMapper.readValue(json, TripState.class);
                    assertThat(retrievedState.getTripId()).isEqualTo(tripId);
                    assertThat(retrievedState.getTripStatus()).isEqualTo(TripStatus.AT_STOP);
                    assertThat(retrievedState.getStopPredictions()).hasSize(1);
                    assertThat(retrievedState.getStopPredictions().get(0).getStopId()).isEqualTo("stop-13");
                });
    }

    private void waitForPersistence(java.util.function.BooleanSupplier condition) throws InterruptedException {
        int retries = 30;
        while (retries-- > 0) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("Condition not met after waiting.");
    }
}
