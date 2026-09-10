package com.transit.reliability.service;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.transit.reliability.model.TripUpdateEvent;
import com.transit.reliability.parser.MtaGtfsRealtimeMapper;
import com.transit.reliability.parser.MtaGtfsRealtimeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MtaRealtimePoller {

    private static final Logger log = LoggerFactory.getLogger(MtaRealtimePoller.class);

    private final MtaGtfsRealtimeParser parser;
    private final MtaGtfsRealtimeMapper mapper;
    private final TripUpdateProducer producer;
    private final HttpClient httpClient;

    private final String feedUrl;
    private final String apiKey;
    private final boolean enabled;
    private final AtomicBoolean isPolling = new AtomicBoolean(false);

    @Autowired
    public MtaRealtimePoller(
            MtaGtfsRealtimeParser parser,
            MtaGtfsRealtimeMapper mapper,
            TripUpdateProducer producer,
            @Value("${mta.realtime.feed-url}") String feedUrl,
            @Value("${mta.realtime.api-key:}") String apiKey,
            @Value("${mta.realtime.enabled:true}") boolean enabled) {
        this(
                parser,
                mapper,
                producer,
                feedUrl,
                apiKey,
                enabled,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
        );
    }

    public MtaRealtimePoller(
            MtaGtfsRealtimeParser parser,
            MtaGtfsRealtimeMapper mapper,
            TripUpdateProducer producer,
            String feedUrl,
            boolean enabled,
            HttpClient httpClient) {
        this(parser, mapper, producer, feedUrl, "", enabled, httpClient);
    }

    public MtaRealtimePoller(
            MtaGtfsRealtimeParser parser,
            MtaGtfsRealtimeMapper mapper,
            TripUpdateProducer producer,
            String feedUrl,
            String apiKey,
            boolean enabled,
            HttpClient httpClient) {
        this.parser = parser;
        this.mapper = mapper;
        this.producer = producer;
        this.feedUrl = feedUrl;
        this.apiKey = apiKey;
        this.enabled = enabled;
        this.httpClient = httpClient;
    }

    @Scheduled(fixedDelayString = "${mta.realtime.polling-interval:30s}")
    public void poll() {
        if (!enabled) {
            log.debug("MTA realtime polling is disabled.");
            return;
        }

        if (!isPolling.compareAndSet(false, true)) {
            log.warn("MTA realtime polling is already in progress. Skipping this execution cycle.");
            return;
        }

        log.info("Starting MTA realtime feed poll from: {}", feedUrl);
        int parsedCount = 0;
        int publishedCount = 0;

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(feedUrl))
                    .GET();

            if (apiKey != null && !apiKey.trim().isEmpty()) {
                requestBuilder.header("x-api-key", apiKey.trim());
            }

            HttpRequest request = requestBuilder.build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("Failed to fetch MTA feed. HTTP status code: " + response.statusCode());
            }

            Optional<String> contentTypeOpt = response.headers().firstValue("Content-Type");
            boolean isTextOrMarkup = false;
            String contentType = "";
            if (contentTypeOpt.isPresent()) {
                contentType = contentTypeOpt.get().toLowerCase();
                if (contentType.contains("xml") || contentType.contains("html") || contentType.contains("json")) {
                    isTextOrMarkup = true;
                }
            }

            try (InputStream rawBody = response.body();
                 BufferedInputStream bis = new BufferedInputStream(rawBody)) {

                bis.mark(1024);
                byte[] previewBytes = new byte[1024];
                int bytesRead = bis.read(previewBytes);
                bis.reset();

                String previewStr = "";
                if (bytesRead > 0) {
                    previewStr = new String(previewBytes, 0, bytesRead, StandardCharsets.UTF_8).trim();
                    String lowerPreview = previewStr.toLowerCase();
                    if (lowerPreview.startsWith("<") || lowerPreview.startsWith("{") || lowerPreview.startsWith("[") || 
                        lowerPreview.contains("<html") || lowerPreview.contains("<!doctype") || lowerPreview.contains("<error")) {
                        isTextOrMarkup = true;
                    }
                }

                if (isTextOrMarkup) {
                    String errorMsg = "MTA feed returned non-binary/text response. " +
                            (!contentType.isEmpty() ? "Content-Type: " + contentType + ". " : "") +
                            "Body preview: " + (previewStr.length() > 300 ? previewStr.substring(0, 300) + "..." : previewStr);
                    throw new IOException(errorMsg);
                }

                FeedMessage feedMessage = parser.parseFeedMessage(bis);
                FeedHeader header = feedMessage.getHeader();

                for (FeedEntity entity : feedMessage.getEntityList()) {
                    if (entity.hasTripUpdate()) {
                        TripUpdate tripUpdate = entity.getTripUpdate();
                        parsedCount++;
                        try {
                            TripUpdateEvent event = mapper.mapToTripUpdateEvent(tripUpdate, header);
                            producer.sendTripUpdateEvent(event);
                            publishedCount++;
                        } catch (Exception e) {
                            log.error("Failed to map or publish TripUpdate for tripId={}: {}", 
                                    tripUpdate.getTrip().getTripId(), e.getMessage(), e);
                        }
                    }
                }
            }
            log.info("MTA realtime feed poll completed. Parsed {} TripUpdates. Successfully published {} events.", 
                    parsedCount, publishedCount);

        } catch (Exception e) {
            log.error("Error during MTA realtime feed poll: {}", e.getMessage(), e);
        } finally {
            isPolling.set(false);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getFeedUrl() {
        return feedUrl;
    }
}
