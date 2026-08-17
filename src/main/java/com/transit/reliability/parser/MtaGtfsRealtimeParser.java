package com.transit.reliability.parser;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

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
        
        if (feedMessage.getHeader().hasTimestamp()) {
            System.out.println("FeedHeader timestamp: " + feedMessage.getHeader().getTimestamp());
        } else {
            System.out.println("FeedHeader timestamp: not present");
        }

        for (FeedEntity entity : feedMessage.getEntityList()) {
            if (entity.hasTripUpdate()) {
                TripUpdate tripUpdate = entity.getTripUpdate();
                if (tripUpdate.hasTimestamp()) {
                    System.out.println("TripUpdate timestamp: " + tripUpdate.getTimestamp());
                } else {
                    System.out.println("TripUpdate timestamp: not present");
                }
                
                if (tripUpdate.getTrip().hasTripId()) {
                    System.out.println("TripId: " + tripUpdate.getTrip().getTripId());
                } else {
                    System.out.println("TripId: not present");
                }
                
                return Optional.of(tripUpdate);
            }
        }
        return Optional.empty();
    }
}
