package com.transit.reliability.service;

import com.transit.reliability.config.KafkaConfig;
import com.transit.reliability.config.KafkaTopicProperties;
import com.transit.reliability.model.ServiceAlertEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ServiceAlertProducer {

    private static final Logger log =
            LoggerFactory.getLogger(ServiceAlertProducer.class);

    private final KafkaTemplate<String, ServiceAlertEvent> kafkaTemplate;

    private final KafkaTopicProperties topics;

    public ServiceAlertProducer(
            KafkaTemplate<String, ServiceAlertEvent> kafkaTemplate,
            KafkaTopicProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    public CompletableFuture<SendResult<String, ServiceAlertEvent>>
    sendServiceAlertEvent(ServiceAlertEvent event) {

        log.info(
                "Publishing ServiceAlertEvent to Kafka: alertId={}, type={}",
                event.alertId(),
                event.alertType()
        );

        CompletableFuture<SendResult<String, ServiceAlertEvent>> future =
                kafkaTemplate.send(
                        topics.serviceAlert(),
                        event.alertId(),
                        event
                );

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error(
                        "Failed to publish ServiceAlertEvent with key={}: {}",
                        event.alertId(),
                        ex.getMessage()
                );
            } else {
                log.info(
                        "Successfully published ServiceAlertEvent with key={} to partition {} and offset {}",
                        event.alertId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                );
            }
        });

        return future;
    }
}