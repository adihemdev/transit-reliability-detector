package com.transit.reliability.parser;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.transit.reliability.model.StopTimingUpdate;
import com.transit.reliability.model.TransportMode;
import com.transit.reliability.model.TripUpdateEvent;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class MtaGtfsRealtimeMapperTest {

    @Test
    public void testMapToTripUpdateEventWithRealFixture() throws Exception {
        MtaGtfsRealtimeParser parser = new MtaGtfsRealtimeParser();
        MtaGtfsRealtimeMapper mapper = new MtaGtfsRealtimeMapper();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("mta-ace.pb")) {
            assertThat(inputStream).describedAs("mta-ace.pb resource should be found").isNotNull();

            // Read the full FeedMessage to extract the header timestamp
            byte[] feedBytes = inputStream.readAllBytes();
            
            // Re-create stream for parser
            java.io.ByteArrayInputStream bis1 = new java.io.ByteArrayInputStream(feedBytes);
            Optional<TripUpdate> tripUpdateOpt = parser.parseFirstTripUpdate(bis1);
            assertThat(tripUpdateOpt).isPresent();
            TripUpdate tripUpdate = tripUpdateOpt.get();

            java.io.ByteArrayInputStream bis2 = new java.io.ByteArrayInputStream(feedBytes);
            FeedMessage feedMessage = FeedMessage.parseFrom(bis2);
            FeedHeader header = feedMessage.getHeader();

            // 1. Perform normalization mapping
            TripUpdateEvent event = mapper.mapToTripUpdateEvent(tripUpdate, header);

            // 2. Print one normalized TripUpdateEvent produced from the real fixture
            System.out.println("=== NORMALIZED TRIP UPDATE EVENT FROM FIXTURE ===");
            System.out.println(event);
            System.out.println("=================================================");

            // 3. Verify assertions
            
            // Prove: tripId and routeId map correctly
            assertThat(event.getTripId()).isEqualTo("006200_A..N09R");
            assertThat(event.getRouteId()).isEqualTo("A");
            assertThat(event.getTransportMode()).isEqualTo(TransportMode.SUBWAY);

            // Prove: eventTimestamp falls back to FeedHeader.timestamp (since first TripUpdate has no timestamp)
            assertThat(tripUpdate.hasTimestamp()).isFalse();
            assertThat(header.hasTimestamp()).isTrue();
            long expectedTimestampSec = header.getTimestamp();
            assertThat(event.getEventTimestamp()).isEqualTo(Instant.ofEpochSecond(expectedTimestampSec));

            // Prove: all stop_time_updates become StopTimingUpdate entries
            assertThat(event.getStopUpdates()).hasSize(tripUpdate.getStopTimeUpdateCount());
            assertThat(event.getStopUpdates().size()).isGreaterThan(0);

            // Prove: predicted times map correctly and scheduled times remain absent
            for (int i = 0; i < tripUpdate.getStopTimeUpdateCount(); i++) {
                var protoUpdate = tripUpdate.getStopTimeUpdate(i);
                StopTimingUpdate mappedUpdate = event.getStopUpdates().get(i);

                // stopId
                assertThat(mappedUpdate.getStopId()).isEqualTo(protoUpdate.getStopId());

                // predicted times mapping
                if (protoUpdate.hasArrival() && protoUpdate.getArrival().hasTime()) {
                    assertThat(mappedUpdate.getPredictedArrival()).isEqualTo(Instant.ofEpochSecond(protoUpdate.getArrival().getTime()));
                } else {
                    assertThat(mappedUpdate.getPredictedArrival()).isNull();
                }

                if (protoUpdate.hasDeparture() && protoUpdate.getDeparture().hasTime()) {
                    assertThat(mappedUpdate.getPredictedDeparture()).isEqualTo(Instant.ofEpochSecond(protoUpdate.getDeparture().getTime()));
                } else {
                    assertThat(mappedUpdate.getPredictedDeparture()).isNull();
                }

                // scheduled times remain absent (null/unset)
                assertThat(mappedUpdate.getScheduledArrival()).isNull();
                assertThat(mappedUpdate.getScheduledDeparture()).isNull();
            }
        }
    }

    @Test
    public void testEventTimestampPrefersTripUpdateTimestamp() {
        MtaGtfsRealtimeMapper mapper = new MtaGtfsRealtimeMapper();

        // Construct mock/custom protobuf data to explicitly test preference when TripUpdate has its own timestamp
        long headerTime = 1000L;
        long tripUpdateTime = 2000L;

        TripUpdate tripUpdateWithTimestamp = TripUpdate.newBuilder()
                .setTrip(com.google.transit.realtime.GtfsRealtime.TripDescriptor.newBuilder().setTripId("test-trip").setRouteId("A").build())
                .setTimestamp(tripUpdateTime)
                .build();

        FeedHeader header = FeedHeader.newBuilder()
                .setGtfsRealtimeVersion("2.0")
                .setTimestamp(headerTime)
                .build();

        TripUpdateEvent event = mapper.mapToTripUpdateEvent(tripUpdateWithTimestamp, header);

        // Prove: eventTimestamp prefers TripUpdate's specific timestamp when present
        assertThat(event.getEventTimestamp()).isEqualTo(Instant.ofEpochSecond(tripUpdateTime));
    }
}
