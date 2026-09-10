package com.transit.reliability.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

@Configuration
public class AwsLambdaConfig {

    @Bean
    public LambdaClient lambdaClient(AwsProperties properties) {
        return LambdaClient.builder()
                .region(Region.of(properties.region()))
                .build();
    }
}