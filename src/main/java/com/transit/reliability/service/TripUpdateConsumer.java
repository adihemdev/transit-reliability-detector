package com.transit.reliability.service;

import com.transit.reliability.config.KafkaConfig;
import com.transit.reliability.model.StopTimingUpdate;
import com.transit.reliability.model.TripState;
import com.transit.reliability.model.TripStatus;
import com.transit.reliability.model.TripStopPrediction;
import com.transit.reliability.model.TripUpdateEvent;
import com.transit.reliability.repository.TripStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class TripUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(TripUpdateConsumer.class);

    private final TripStateRepository tripStateRepository;
    private final List<TripUpdateEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());

    public TripUpdateConsumer(TripStateRepository tripStateRepository) {
        this.tripStateRepository = tripStateRepository;
    }

    @KafkaListener(topics = KafkaConfig.TRIP_UPDATE_TOPIC, groupId = "${spring.kafka.consumer.group-id:transit-reliability-group}")
    @Transactional
    public void consumeTripUpdateEvent(TripUpdateEvent event) {
        if (event == null || event.getTripId() == null) {
            log.warn("Received null or invalid TripUpdateEvent");
            return;
        }

        log.info("Received TripUpdateEvent from Kafka: tripId={}, routeId={}, stopUpdatesCount={}, timestamp={}",
                event.getTripId(), event.getRouteId(), 
                event.getStopUpdates() != null ? event.getStopUpdates().size() : 0, 
                event.getEventTimestamp());
        
        // Track the event for integration test assertions
        receivedEvents.add(event);

        // Out-of-order & idempotent current-state persistence (PostgreSQL)
        Optional<TripState> existingStateOpt = tripStateRepository.findById(event.getTripId());
        if (existingStateOpt.isPresent()) {
            TripState existingState = existingStateOpt.get();
            if (event.getEventTimestamp().isBefore(existingState.getLastUpdated())) {
                log.warn("Out-of-order event ignored for trip current-state: tripId={}. Event timestamp {} is older than existing state timestamp {}.",
                        event.getTripId(), event.getEventTimestamp(), existingState.getLastUpdated());
                return;
            }
        }

        TripState state = existingStateOpt.orElse(new TripState());
        if (existingStateOpt.isEmpty()) {
            state.setTripId(event.getTripId());
            state.setTripStatus(TripStatus.IN_TRANSIT);
            state.setCompletedStops(0);
        }
        state.setRouteId(event.getRouteId());
        state.setTransportMode(event.getTransportMode());
        state.setLastUpdated(event.getEventTimestamp());

        // Clear existing predictions and flush to execute deletes before any inserts with same PKs
        state.getStopPredictions().clear();
        tripStateRepository.saveAndFlush(state);

        // Build new predictions list
        List<TripStopPrediction> newPredictions = new ArrayList<>();
        int sequence = 1;
        if (event.getStopUpdates() != null) {
            for (StopTimingUpdate update : event.getStopUpdates()) {
                TripStopPrediction prediction = new TripStopPrediction(
                        event.getTripId(),
                        sequence++,
                        update.getStopId(),
                        update.getPredictedArrival(),
                        update.getPredictedDeparture()
                );
                prediction.setTripState(state);
                newPredictions.add(prediction);
            }
        }

        // Add new predictions and save/flush again
        state.getStopPredictions().addAll(newPredictions);
        tripStateRepository.saveAndFlush(state);
        
        log.info("Successfully updated current-state and predictions for tripId={} with timestamp {}", 
                event.getTripId(), event.getEventTimestamp());
    }

    public List<TripUpdateEvent> getReceivedEvents() {
        return new ArrayList<>(receivedEvents);
    }

    public void clearReceivedEvents() {
        receivedEvents.clear();
    }
}
