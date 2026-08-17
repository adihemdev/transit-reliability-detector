package com.transit.reliability.service;

import com.transit.reliability.config.KafkaConfig;
import com.transit.reliability.model.TripState;
import com.transit.reliability.model.TripTimingEvent;
import com.transit.reliability.model.TripUpdate;
import com.transit.reliability.model.TripUpdateId;
import com.transit.reliability.repository.TripStateRepository;
import com.transit.reliability.repository.TripUpdateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class TripTimingConsumer {

    private static final Logger log = LoggerFactory.getLogger(TripTimingConsumer.class);

    private final TripStateRepository tripStateRepository;
    private final TripUpdateRepository tripUpdateRepository;

    public TripTimingConsumer(TripStateRepository tripStateRepository, TripUpdateRepository tripUpdateRepository) {
        this.tripStateRepository = tripStateRepository;
        this.tripUpdateRepository = tripUpdateRepository;
    }

    @KafkaListener(topics = KafkaConfig.TRIP_TIMING_TOPIC, groupId = "${spring.kafka.consumer.group-id:transit-reliability-group}")
    @Transactional
    public void consumeTripTimingEvent(TripTimingEvent event) {
        log.info("Received TripTimingEvent from Kafka: tripId={}, routeId={}, status={}, timestamp={}",
                event.getTripId(), event.getRouteId(), event.getTripStatus(), event.getEventTimestamp());

        // 1. Idempotent historical data persistence (TimescaleDB)
        TripUpdateId updateId = new TripUpdateId(event.getTripId(), event.getEventTimestamp());
        boolean updateExists = tripUpdateRepository.existsById(updateId);
        
        if (!updateExists) {
            TripUpdate tripUpdate = TripUpdate.fromEvent(event);
            tripUpdateRepository.save(tripUpdate);
            log.info("Persisted historical TripUpdate to TimescaleDB for tripId={} at {}", 
                    event.getTripId(), event.getEventTimestamp());
        } else {
            log.info("Duplicate historical TripUpdate detected and skipped for tripId={} at {}", 
                    event.getTripId(), event.getEventTimestamp());
        }

        // 2. Out-of-order & idempotent current-state persistence (PostgreSQL)
        Optional<TripState> existingStateOpt = tripStateRepository.findById(event.getTripId());
        if (existingStateOpt.isPresent()) {
            TripState existingState = existingStateOpt.get();
            if (event.getEventTimestamp().isBefore(existingState.getLastUpdated())) {
                log.warn("Out-of-order event ignored for trip current-state: tripId={}. Event timestamp {} is older than existing state timestamp {}.",
                        event.getTripId(), event.getEventTimestamp(), existingState.getLastUpdated());
            } else {
                TripState updatedState = TripState.fromEvent(event);
                tripStateRepository.save(updatedState);
                log.info("Updated current-state for tripId={} with newer event timestamp {}", 
                        event.getTripId(), event.getEventTimestamp());
            }
        } else {
            TripState newState = TripState.fromEvent(event);
            tripStateRepository.save(newState);
            log.info("Created new current-state for tripId={} with timestamp {}", 
                    event.getTripId(), event.getEventTimestamp());
        }
    }
}
