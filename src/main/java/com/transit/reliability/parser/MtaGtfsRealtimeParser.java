package com.transit.reliability.parser;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Component
public class MtaGtfsRealtimeParser {

    private static final Logger log = LoggerFactory.getLogger(MtaGtfsRealtimeParser.class);

    /**
     * Parses the GTFS-Realtime protobuf feed from an InputStream and returns the first TripUpdate.
     *
     * @param inputStream the stream containing the GTFS-Realtime protobuf data
     * @return an Optional containing the first found TripUpdate, or empty if none exist
     * @throws IOException if parsing fails
     */
    public Optional<TripUpdate> parseFirstTripUpdate(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        FeedMessage feedMessage = FeedMessage.parseFrom(inputStream);
        
        if (!feedMessage.getHeader().hasTimestamp()) {
            log.info("FeedHeader timestamp: not present");
        }

        for (FeedEntity entity : feedMessage.getEntityList()) {
            if (entity.hasTripUpdate()) {
                TripUpdate tripUpdate = entity.getTripUpdate();
                if (!tripUpdate.hasTimestamp()) {
                    log.info("TripUpdate timestamp: not present");
                }
                
                if (!tripUpdate.getTrip().hasTripId()) {
                    log.warn("TripId: not present");
                }
                
                return Optional.of(tripUpdate);
            }
        }
        return Optional.empty();
    }

    /**
     * Parses the GTFS-Realtime protobuf feed from an InputStream and returns the parsed FeedMessage.
     *
     * @param inputStream the stream containing the GTFS-Realtime protobuf data
     * @return the parsed FeedMessage
     * @throws IOException if parsing fails
     */
    public FeedMessage parseFeedMessage(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        return FeedMessage.parseFrom(inputStream);
    }
}
