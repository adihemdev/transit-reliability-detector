package com.transit.reliability.parser;

import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.transit.reliability.model.StopTimingUpdate;
import com.transit.reliability.model.TransportMode;
import com.transit.reliability.model.TripUpdateEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps a parsed MTA GTFS-Realtime TripUpdate into our normalized internal TripUpdateEvent.
 */
public class MtaGtfsRealtimeMapper {

    /**
     * Maps a parsed MTA GTFS-Realtime TripUpdate and FeedHeader into our normalized internal TripUpdateEvent.
     *
     * @param tripUpdate the parsed TripUpdate
     * @param header     the feed header containing feed metadata/timestamp
     * @return the mapped TripUpdateEvent
     */
    public TripUpdateEvent mapToTripUpdateEvent(TripUpdate tripUpdate, FeedHeader header) {
        if (tripUpdate == null) {
            throw new IllegalArgumentException("TripUpdate cannot be null");
        }
        if (header == null) {
            throw new IllegalArgumentException("FeedHeader cannot be null");
        }
        long headerTimestamp = header.hasTimestamp() ? header.getTimestamp() : 0L;
        return mapToTripUpdateEvent(tripUpdate, headerTimestamp);
    }

    /**
     * Maps a parsed MTA GTFS-Realtime TripUpdate and a primitive header timestamp into our normalized internal TripUpdateEvent.
     *
     * @param tripUpdate           the parsed TripUpdate
     * @param feedHeaderTimestamp  the feed header timestamp in seconds from epoch
     * @return the mapped TripUpdateEvent
     */
    public TripUpdateEvent mapToTripUpdateEvent(TripUpdate tripUpdate, long feedHeaderTimestamp) {
        if (tripUpdate == null) {
            throw new IllegalArgumentException("TripUpdate cannot be null");
        }

        String tripId = null;
        String routeId = null;
        if (tripUpdate.hasTrip()) {
            if (tripUpdate.getTrip().hasTripId()) {
                tripId = tripUpdate.getTrip().getTripId();
            }
            if (tripUpdate.getTrip().hasRouteId()) {
                routeId = tripUpdate.getTrip().getRouteId();
            }
        }

        TransportMode transportMode = TransportMode.SUBWAY;

        Instant eventTimestamp;
        if (tripUpdate.hasTimestamp() && tripUpdate.getTimestamp() > 0) {
            eventTimestamp = Instant.ofEpochSecond(tripUpdate.getTimestamp());
        } else {
            eventTimestamp = Instant.ofEpochSecond(feedHeaderTimestamp);
        }

        List<StopTimingUpdate> stopUpdates = new ArrayList<>();
        for (StopTimeUpdate stu : tripUpdate.getStopTimeUpdateList()) {
            String stopId = stu.hasStopId() ? stu.getStopId() : null;

            Instant predictedArrival = null;
            if (stu.hasArrival() && stu.getArrival().hasTime()) {
                predictedArrival = Instant.ofEpochSecond(stu.getArrival().getTime());
            }

            Instant predictedDeparture = null;
            if (stu.hasDeparture() && stu.getDeparture().hasTime()) {
                predictedDeparture = Instant.ofEpochSecond(stu.getDeparture().getTime());
            }

            // Scheduled times are not available from this feed yet
            Instant scheduledArrival = null;
            Instant scheduledDeparture = null;

            StopTimingUpdate stopUpdate = new StopTimingUpdate(
                    stopId,
                    scheduledArrival,
                    predictedArrival,
                    scheduledDeparture,
                    predictedDeparture
            );
            stopUpdates.add(stopUpdate);
        }

        return new TripUpdateEvent(tripId, routeId, transportMode, eventTimestamp, stopUpdates);
    }
}
