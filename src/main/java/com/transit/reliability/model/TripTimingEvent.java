package com.transit.reliability.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class TripTimingEvent {

    @NotBlank(message = "tripId is required")
    private String tripId;

    @NotBlank(message = "routeId is required")
    private String routeId;

    @NotBlank(message = "stopId is required")
    private String stopId;

    @NotNull(message = "transportMode is required")
    private TransportMode transportMode;

    private Instant scheduledArrival;
    private Instant predictedArrival;
    private Instant scheduledDeparture;
    private Instant predictedDeparture;

    @NotNull(message = "tripStatus is required")
    private TripStatus tripStatus;

    private Integer completedStops;

    @NotNull(message = "eventTimestamp is required")
    private Instant eventTimestamp;

    // Default constructor for Jackson
    public TripTimingEvent() {
    }

    // All-args constructor
    public TripTimingEvent(String tripId, String routeId, String stopId, TransportMode transportMode,
                            Instant scheduledArrival, Instant predictedArrival,
                            Instant scheduledDeparture, Instant predictedDeparture,
                            TripStatus tripStatus, Integer completedStops, Instant eventTimestamp) {
        this.tripId = tripId;
        this.routeId = routeId;
        this.stopId = stopId;
        this.transportMode = transportMode;
        this.scheduledArrival = scheduledArrival;
        this.predictedArrival = predictedArrival;
        this.scheduledDeparture = scheduledDeparture;
        this.predictedDeparture = predictedDeparture;
        this.tripStatus = tripStatus;
        this.completedStops = completedStops;
        this.eventTimestamp = eventTimestamp;
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

    public String getStopId() {
        return stopId;
    }

    public void setStopId(String stopId) {
        this.stopId = stopId;
    }

    public TransportMode getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(TransportMode transportMode) {
        this.transportMode = transportMode;
    }

    public Instant getScheduledArrival() {
        return scheduledArrival;
    }

    public void setScheduledArrival(Instant scheduledArrival) {
        this.scheduledArrival = scheduledArrival;
    }

    public Instant getPredictedArrival() {
        return predictedArrival;
    }

    public void setPredictedArrival(Instant predictedArrival) {
        this.predictedArrival = predictedArrival;
    }

    public Instant getScheduledDeparture() {
        return scheduledDeparture;
    }

    public void setScheduledDeparture(Instant scheduledDeparture) {
        this.scheduledDeparture = scheduledDeparture;
    }

    public Instant getPredictedDeparture() {
        return predictedDeparture;
    }

    public void setPredictedDeparture(Instant predictedDeparture) {
        this.predictedDeparture = predictedDeparture;
    }

    public TripStatus getTripStatus() {
        return tripStatus;
    }

    public void setTripStatus(TripStatus tripStatus) {
        this.tripStatus = tripStatus;
    }

    public Integer getCompletedStops() {
        return completedStops;
    }

    public void setCompletedStops(Integer completedStops) {
        this.completedStops = completedStops;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    @Override
    public String toString() {
        return "TripTimingEvent{" +
                "tripId='" + tripId + '\'' +
                ", routeId='" + routeId + '\'' +
                ", stopId='" + stopId + '\'' +
                ", transportMode=" + transportMode +
                ", scheduledArrival=" + scheduledArrival +
                ", predictedArrival=" + predictedArrival +
                ", scheduledDeparture=" + scheduledDeparture +
                ", predictedDeparture=" + predictedDeparture +
                ", tripStatus=" + tripStatus +
                ", completedStops=" + completedStops +
                ", eventTimestamp=" + eventTimestamp +
                '}';
    }
}
