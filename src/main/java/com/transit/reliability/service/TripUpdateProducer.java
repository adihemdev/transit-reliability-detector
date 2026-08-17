package com.transit.reliability.service;

import com.transit.reliability.config.KafkaConfig;
import com.transit.reliability.model.TripUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class TripUpdateProducer {

    private static final Logger log = LoggerFactory.getLogger(TripUpdateProducer.class);

    private final KafkaTemplate<String, TripUpdateEvent> kafkaTemplate;

    public TripUpdateProducer(KafkaTemplate<String, TripUpdateEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, TripUpdateEvent>> sendTripUpdateEvent(TripUpdateEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("TripUpdateEvent cannot be null");
        }
        log.info("Publishing TripUpdateEvent to Kafka: tripId={}, routeId={}, timestamp={}", 
                event.getTripId(), event.getRouteId(), event.getEventTimestamp());
        
        CompletableFuture<SendResult<String, TripUpdateEvent>> future = 
                kafkaTemplate.send(KafkaConfig.TRIP_UPDATE_TOPIC, event.getTripId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish TripUpdateEvent with key={}: {}", event.getTripId(), ex.getMessage());
            } else {
                log.info("Successfully published TripUpdateEvent with key={} to partition {} and offset {}", 
                        event.getTripId(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });

        return future;
    }
}
