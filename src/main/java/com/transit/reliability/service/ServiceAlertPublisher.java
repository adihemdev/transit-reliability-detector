package com.transit.reliability.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.transit.reliability.model.ServiceAlertEvent;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceAlertPublisher {

    private final MtaServiceAlertFeedParser parser;
    private final ServiceAlertProducer producer;

    public ServiceAlertPublisher(
            MtaServiceAlertFeedParser parser,
            ServiceAlertProducer producer) {
        this.parser = parser;
        this.producer = producer;
    }

    public void publishFeed(String json) throws JsonProcessingException {
        List<ServiceAlertEvent> alerts = parser.parse(json);

        for (ServiceAlertEvent alert : alerts) {
            producer.sendServiceAlertEvent(alert);
        }
    }
}