package com.transit.reliability.service;


import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class KafkaProducerMetricsLogger {

    private static final Set<String> METRICS = Set.of(
            "records-per-request-avg",
            "batch-size-avg",
            "compression-rate-avg",
            "record-queue-time-avg",
            "record-send-rate",
            "request-latency-avg"
    );

    private final KafkaTemplate<?, ?> kafkaTemplate;

    public KafkaProducerMetricsLogger(KafkaTemplate<?, ?> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 10_000)
    public void logMetrics() {

        kafkaTemplate.metrics().forEach((name, metric) -> {

            if (METRICS.contains(name.name())
                    && "producer-metrics".equals(name.group())) {

                System.out.printf(
                        "%s = %s%n",
                        name.name(),
                        metric.metricValue()
                );
            }
        });
    }
}
