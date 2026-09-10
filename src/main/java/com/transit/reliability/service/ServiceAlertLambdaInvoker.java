package com.transit.reliability.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transit.reliability.config.AwsProperties;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;

@Slf4j
@Service
public class ServiceAlertLambdaInvoker {



    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;
    private final AwsProperties properties;

    public ServiceAlertLambdaInvoker(
            LambdaClient lambdaClient,
            ObjectMapper objectMapper,
            AwsProperties properties) {

        this.lambdaClient = lambdaClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String invoke(Object serviceAlert) {

        String functionName =
                properties.lambda().serviceAlertFunctionName();

        try {
            String payload =
                    objectMapper.writeValueAsString(serviceAlert);

            InvokeRequest request = InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromUtf8String(payload))
                    .build();

            InvokeResponse response =
                    lambdaClient.invoke(request);

            String responsePayload =
                    response.payload().asUtf8String();

            if (response.functionError() != null) {
                throw new IllegalStateException(
                        "Lambda execution failed: " + responsePayload
                );
            }

            return responsePayload;

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Failed to serialize service alert", e
            );

        } catch (LambdaException e) {
            log.error(
                    "Failed to invoke Lambda {}: {}",
                    functionName,
                    e.awsErrorDetails() != null
                            ? e.awsErrorDetails().errorMessage()
                            : e.getMessage()
            );

            throw new IllegalStateException(
                    "Failed to invoke service-alert Lambda", e
            );
        }
    }
}