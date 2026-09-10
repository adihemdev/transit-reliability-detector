package com.transit.reliability.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicProperties(
        String tripTiming,
        String tripUpdate,
        String tripUpdateDlt,
        String serviceAlert
) {
}
