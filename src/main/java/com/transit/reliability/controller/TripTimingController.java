package com.transit.reliability.controller;

import com.transit.reliability.model.TripState;
import com.transit.reliability.model.TripTimingEvent;
import com.transit.reliability.repository.TripStateRepository;
import com.transit.reliability.service.TripTimingProducer;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
public class TripTimingController {

    private static final Logger log = LoggerFactory.getLogger(TripTimingController.class);

    private final TripTimingProducer producer;
    private final TripStateRepository tripStateRepository;

    public TripTimingController(TripTimingProducer producer, TripStateRepository tripStateRepository) {
        this.producer = producer;
        this.tripStateRepository = tripStateRepository;
    }

    /**
     * Submit a new TripTimingEvent. It will be published to Kafka for async processing and persistence.
     */
    @PostMapping("/timing")
    public ResponseEntity<String> submitTripTimingEvent(@Valid @RequestBody TripTimingEvent event) {
        log.info("Received request to submit TripTimingEvent for tripId={}", event.getTripId());
        
        producer.sendTripTimingEvent(event);
        
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("Event accepted and queued for tripId: " + event.getTripId());
    }

    /**
     * Retrieve the latest persisted state for a specific trip.
     */
    @GetMapping("/{tripId}")
    public ResponseEntity<TripState> getLatestTripState(@PathVariable String tripId) {
        log.info("Received request to get latest trip state for tripId={}", tripId);
        
        return tripStateRepository.findById(tripId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Trip state not found for tripId={}", tripId);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                });
    }
}
