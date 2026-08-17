package com.transit.reliability.parser;

import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class MtaGtfsRealtimeParserTest {

    @Test
    public void testParseFirstTripUpdateFromMtaAceFeed() throws Exception {
        MtaGtfsRealtimeParser parser = new MtaGtfsRealtimeParser();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("mta-ace.pb")) {
            assertThat(inputStream).describedAs("mta-ace.pb resource should be found").isNotNull();

            Optional<TripUpdate> tripUpdateOpt = parser.parseFirstTripUpdate(inputStream);

            assertThat(tripUpdateOpt).isPresent();

            TripUpdate tripUpdate = tripUpdateOpt.get();
            
            // Print the required diagnostic values
            System.out.println("=== DIAGNOSTIC OUTPUT ===");
            // Let's obtain the feed message header again to print in test as well
            try (InputStream is2 = getClass().getClassLoader().getResourceAsStream("mta-ace.pb")) {
                com.google.transit.realtime.GtfsRealtime.FeedMessage feedMessage = com.google.transit.realtime.GtfsRealtime.FeedMessage.parseFrom(is2);
                if (feedMessage.getHeader().hasTimestamp()) {
                    System.out.println("FeedHeader.timestamp: " + feedMessage.getHeader().getTimestamp());
                } else {
                    System.out.println("FeedHeader.timestamp: [not present]");
                }
            }
            if (tripUpdate.hasTimestamp()) {
                System.out.println("TripUpdate.timestamp: " + tripUpdate.getTimestamp());
            } else {
                System.out.println("TripUpdate.timestamp: [not present]");
            }
            System.out.println("TripUpdate's tripId: " + tripUpdate.getTrip().getTripId());
            System.out.println("=========================");

            // Print the TripUpdate to demonstrate successful reading as requested
            System.out.println("=== Parsed TripUpdate ===");
            System.out.println(tripUpdate);
            System.out.println("=========================");

            // Basic assertions to confirm GTFS protobuf properties are parsed
            assertThat(tripUpdate.getTrip()).isNotNull();
            assertThat(tripUpdate.getTrip().getTripId()).isNotEmpty();
            assertThat(tripUpdate.getStopTimeUpdateCount()).isGreaterThan(0);
        }
    }
}
