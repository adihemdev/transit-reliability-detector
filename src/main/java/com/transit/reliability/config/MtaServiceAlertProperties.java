package com.transit.reliability.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "mta.service-alert")
public record MtaServiceAlertProperties(
        String feedUrl,
        String apiKey,
        boolean enabled,
        Duration pollingInterval
) {
}