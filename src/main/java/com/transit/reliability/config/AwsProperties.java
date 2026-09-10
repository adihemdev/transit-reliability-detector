package com.transit.reliability.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public record AwsProperties(
        String region,
        Lambda lambda
) {
    public record Lambda(
            String serviceAlertFunctionName
    ) {}
}