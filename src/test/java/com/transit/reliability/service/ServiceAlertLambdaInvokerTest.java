package com.transit.reliability.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transit.reliability.config.AwsProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServiceAlertLambdaInvokerTest {

    AwsProperties properties =
            new AwsProperties(
                    "us-east-2",
                    new AwsProperties.Lambda(
                            "mta-service-alert-processor"
                    )
            );

    @Test
    void shouldInvokeConfiguredLambdaWithSerializedAlert() throws Exception {

        LambdaClient lambdaClient = mock(LambdaClient.class);

        InvokeResponse invokeResponse = InvokeResponse.builder()
                .payload(SdkBytes.fromUtf8String("{\"status\":\"processed\"}"))
                .build();

        when(lambdaClient.invoke(any(InvokeRequest.class)))
                .thenReturn(invokeResponse);

        ServiceAlertLambdaInvoker invoker =
                new ServiceAlertLambdaInvoker(
                        lambdaClient,
                        new ObjectMapper(),
                        properties

                );

        Map<String, Object> alert = Map.of(
                "routeId", "A",
                "alertType", "DELAY",
                "severity", "HIGH"
        );

        String response = invoker.invoke(alert);

        ArgumentCaptor<InvokeRequest> captor =
                ArgumentCaptor.forClass(InvokeRequest.class);

        verify(lambdaClient).invoke(captor.capture());

        InvokeRequest request = captor.getValue();

        assertEquals(
                "mta-service-alert-processor",
                request.functionName()
        );

        String payload = request.payload().asUtf8String();

        assertTrue(payload.contains("\"routeId\":\"A\""));
        assertTrue(payload.contains("\"alertType\":\"DELAY\""));
        assertTrue(payload.contains("\"severity\":\"HIGH\""));

        assertEquals(
                "{\"status\":\"processed\"}",
                response
        );
    }

    @Test
    void shouldThrowWhenLambdaReturnsFunctionError() {

        LambdaClient lambdaClient = mock(LambdaClient.class);

        InvokeResponse response = InvokeResponse.builder()
                .functionError("Unhandled")
                .payload(SdkBytes.fromUtf8String(
                        "{\"errorMessage\":\"processing failed\"}"
                ))
                .build();

        when(lambdaClient.invoke(any(InvokeRequest.class)))
                .thenReturn(response);

        ServiceAlertLambdaInvoker invoker =
                new ServiceAlertLambdaInvoker(
                        lambdaClient,
                        new ObjectMapper(),
                        properties
                );

        assertThrows(
                IllegalStateException.class,
                () -> invoker.invoke(Map.of("routeId", "A"))
        );
    }

}