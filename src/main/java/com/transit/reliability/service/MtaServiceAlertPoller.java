package com.transit.reliability.service;

import com.transit.reliability.config.MtaServiceAlertProperties;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
public class MtaServiceAlertPoller {


    private final ServiceAlertPublisher publisher;
    private final MtaServiceAlertProperties properties;
    private final HttpClient httpClient;

    public MtaServiceAlertPoller(
            ServiceAlertPublisher publisher,
            MtaServiceAlertProperties properties) {

        this.publisher = publisher;
        this.properties = properties;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Scheduled(fixedDelayString = "${mta.service-alert.polling-interval}")
    public void poll() {

        if (!properties.enabled()) {
            log.debug("MTA service-alert polling is disabled.");
            return;
        }

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(properties.feedUrl()))
                    .timeout(Duration.ofSeconds(15))
                    .GET();

            if (properties.apiKey() != null &&
                    !properties.apiKey().isBlank()) {

                requestBuilder.header(
                        "x-api-key",
                        properties.apiKey().trim()
                );
            }

            HttpResponse<String> response =
                    httpClient.send(
                            requestBuilder.build(),
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() != 200) {
                throw new IOException(
                        "Failed to fetch MTA service-alert feed. HTTP status: "
                                + response.statusCode()
                );
            }

            String json = response.body();

            if (json == null || json.isBlank()) {
                log.warn("MTA service-alert feed returned an empty response.");
                return;
            }

            log.info(
                    "Successfully fetched MTA service-alert feed. Response size={} bytes",
                    json.length()
            );

            publisher.publishFeed(json);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("MTA service-alert polling interrupted.", e);

        } catch (IOException e) {
            log.error(
                    "Error while polling MTA service alerts: {}",
                    e.getMessage(),
                    e
            );
        }
    }
}
