package com.transit.reliability.parser;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Slf4j
public class MtaGtfsRealtimeParser {

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
}
