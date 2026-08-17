package com.transit.reliability.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "trip_updates")
@IdClass(TripUpdateId.class)
public class TripUpdate {

    @Id
    @Column(name = "trip_id")
    private String tripId;

    @Column(name = "route_id", nullable = false)
    private String routeId;

    @Column(name = "stop_id", nullable = false)
    private String stopId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_mode", nullable = false)
    private TransportMode transportMode;

    @Column(name = "scheduled_arrival")
    private Instant scheduledArrival;

    @Column(name = "predicted_arrival")
    private Instant predictedArrival;

    @Column(name = "scheduled_departure")
    private Instant scheduledDeparture;

    @Column(name = "predicted_departure")
    private Instant predictedDeparture;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_status", nullable = false)
    private TripStatus tripStatus;

    @Column(name = "completed_stops")
    private Integer completedStops;

    @Id
    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    // Constructors
    public TripUpdate() {
    }

    public TripUpdate(String tripId, String routeId, String stopId, TransportMode transportMode,
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

    // Static helper to create from a TripTimingEvent
    public static TripUpdate fromEvent(TripTimingEvent event) {
        return new TripUpdate(
                event.getTripId(),
                event.getRouteId(),
                event.getStopId(),
                event.getTransportMode(),
                event.getScheduledArrival(),
                event.getPredictedArrival(),
                event.getScheduledDeparture(),
                event.getPredictedDeparture(),
                event.getTripStatus(),
                event.getCompletedStops(),
                event.getEventTimestamp()
        );
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
}
