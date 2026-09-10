package com.transit.reliability.service;

import com.transit.reliability.config.KafkaConfig;
import com.transit.reliability.model.ServiceAlertEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ServiceAlertConsumer {

    private final ServiceAlertLambdaInvoker lambdaInvoker;

    public ServiceAlertConsumer(
            ServiceAlertLambdaInvoker lambdaInvoker) {
        this.lambdaInvoker = lambdaInvoker;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.service-alert}",
            groupId = "service-alert-lambda-consumer"
    )
    public void consume(ServiceAlertEvent event) {

        lambdaInvoker.invoke(event);
    }
}
