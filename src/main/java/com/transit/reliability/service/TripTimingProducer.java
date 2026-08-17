package com.transit.reliability.service;

import com.transit.reliability.config.KafkaConfig;
import com.transit.reliability.model.TripTimingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class TripTimingProducer {

    private static final Logger log = LoggerFactory.getLogger(TripTimingProducer.class);

    private final KafkaTemplate<String, TripTimingEvent> kafkaTemplate;

    public TripTimingProducer(KafkaTemplate<String, TripTimingEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, TripTimingEvent>> sendTripTimingEvent(TripTimingEvent event) {
        log.info("Publishing TripTimingEvent to Kafka: tripId={}, routeId={}, status={}", 
                event.getTripId(), event.getRouteId(), event.getTripStatus());
        
        CompletableFuture<SendResult<String, TripTimingEvent>> future = 
                kafkaTemplate.send(KafkaConfig.TRIP_TIMING_TOPIC, event.getTripId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish TripTimingEvent with key={}: {}", event.getTripId(), ex.getMessage());
            } else {
                log.info("Successfully published TripTimingEvent with key={} to partition {} and offset {}", 
                        event.getTripId(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });

        return future;
    }
}
