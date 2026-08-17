package com.transit.reliability.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a normalized trip snapshot containing timing predictions for multiple upcoming stops.
 */
public class TripUpdateEvent {

    private String tripId;
    private String routeId;
    private TransportMode transportMode;
    private Instant eventTimestamp;
    private List<StopTimingUpdate> stopUpdates = new ArrayList<>();

    // Default constructor for Jackson
    public TripUpdateEvent() {
    }

    // All-args constructor
    public TripUpdateEvent(String tripId, String routeId, TransportMode transportMode,
                           Instant eventTimestamp, List<StopTimingUpdate> stopUpdates) {
        this.tripId = tripId;
        this.routeId = routeId;
        this.transportMode = transportMode;
        this.eventTimestamp = eventTimestamp;
        this.stopUpdates = stopUpdates != null ? stopUpdates : new ArrayList<>();
    }

    // Getters and Setters
    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public TransportMode getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(TransportMode transportMode) {
        this.transportMode = transportMode;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public List<StopTimingUpdate> getStopUpdates() {
        return stopUpdates;
    }

    public void setStopUpdates(List<StopTimingUpdate> stopUpdates) {
        this.stopUpdates = stopUpdates != null ? stopUpdates : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TripUpdateEvent that = (TripUpdateEvent) o;
        return Objects.equals(tripId, that.tripId) &&
                Objects.equals(routeId, that.routeId) &&
                transportMode == that.transportMode &&
                Objects.equals(eventTimestamp, that.eventTimestamp) &&
                Objects.equals(stopUpdates, that.stopUpdates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tripId, routeId, transportMode, eventTimestamp, stopUpdates);
    }

    @Override
    public String toString() {
        return "TripUpdateEvent{" +
                "tripId='" + tripId + '\'' +
                ", routeId='" + routeId + '\'' +
                ", transportMode=" + transportMode +
                ", eventTimestamp=" + eventTimestamp +
                ", stopUpdates=" + stopUpdates +
                '}';
    }
}
