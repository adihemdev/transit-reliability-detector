package com.transit.reliability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transit.reliability.config.AwsProperties;
import com.transit.reliability.service.ServiceAlertLambdaInvoker;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceAlertLambdaInvokerIntegrationTest {

    @Test
    void shouldInvokeRealLambda() throws Exception {

        AwsProperties properties =
                new AwsProperties(
                        "us-east-2",
                        new AwsProperties.Lambda(
                                "mta-service-alert-processor"
                        )
                );

        try (LambdaClient lambdaClient = LambdaClient.builder()
                .region(Region.US_EAST_2)
                .build()) {

            ServiceAlertLambdaInvoker invoker =
                    new ServiceAlertLambdaInvoker(
                            lambdaClient,
                            new ObjectMapper(),
                            properties
                    );

            Map<String, Object> alert = Map.of(
                    "routeId", "A",
                    "alertType", "DELAY",
                    "severity", "HIGH",
                    "description", "Northbound delays due to signal problems"
            );

            String response = invoker.invoke(alert);

            System.out.println("Lambda response: " + response);

            assertTrue(response.contains("\"routeId\":\"A\""));
            assertTrue(response.contains("\"severity\":\"HIGH\""));
        }
    }
}