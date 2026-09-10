package com.transit.reliability.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic tripTimingTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(topics.tripTiming())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic tripUpdateTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(topics.tripUpdate())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic tripUpdateDltTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(topics.tripUpdateDlt())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic serviceAlertTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(topics.serviceAlert())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate,
            KafkaTopicProperties topics) {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(
                                        topics.tripUpdateDlt(),
                                        record.partition()
                                )
                );

        ExponentialBackOffWithMaxRetries backOff =
                new ExponentialBackOffWithMaxRetries(3);

        backOff.setInitialInterval(500L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(2000L);

        return new DefaultErrorHandler(
                recoverer,
                backOff
        );
    }


}
