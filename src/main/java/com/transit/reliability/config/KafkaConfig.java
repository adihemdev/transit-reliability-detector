package com.transit.reliability.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TRIP_TIMING_TOPIC = "trip-timing";
    public static final String TRIP_UPDATE_TOPIC = "trip-update";

    @Bean
    public NewTopic tripTimingTopic() {
        return TopicBuilder.name(TRIP_TIMING_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic tripUpdateTopic() {
        return TopicBuilder.name(TRIP_UPDATE_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
