package com.transit.reliability.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trip_states")
public class TripState {

    @Id
    @Column(name = "trip_id")
    private String tripId;

    @Column(name = "route_id", nullable = false)
    private String routeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_mode", nullable = false)
    private TransportMode transportMode;

    @Column(name = "scheduled_arrival")
    private Instant scheduledArrival;

    @Column(name = "scheduled_departure")
    private Instant scheduledDeparture;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_status", nullable = false)
    private TripStatus tripStatus;

    @Column(name = "completed_stops")
    private Integer completedStops;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    @OneToMany(mappedBy = "tripState", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TripStopPrediction> stopPredictions = new ArrayList<>();

    // Constructors
    public TripState() {
    }

    public TripState(String tripId, String routeId, TransportMode transportMode,
                     Instant scheduledArrival, Instant scheduledDeparture,
                     TripStatus tripStatus, Integer completedStops, Instant lastUpdated) {
        this.tripId = tripId;
        this.routeId = routeId;
        this.transportMode = transportMode;
        this.scheduledArrival = scheduledArrival;
        this.scheduledDeparture = scheduledDeparture;
        this.tripStatus = tripStatus;
        this.completedStops = completedStops;
        this.lastUpdated = lastUpdated;
    }

    // Static helper to create or update from a TripTimingEvent
    public static TripState fromEvent(TripTimingEvent event) {
        TripState state = new TripState(
                event.getTripId(),
                event.getRouteId(),
                event.getTransportMode(),
                event.getScheduledArrival(),
                event.getScheduledDeparture(),
                event.getTripStatus(),
                event.getCompletedStops(),
                event.getEventTimestamp()
        );

        // Map the single flat stop into the stop predictions list
        TripStopPrediction prediction = new TripStopPrediction(
                event.getTripId(),
                1,
                event.getStopId(),
                event.getPredictedArrival(),
                event.getPredictedDeparture()
        );
        state.setStopPredictions(List.of(prediction));
        return state;
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

    public Instant getScheduledArrival() {
        return scheduledArrival;
    }

    public void setScheduledArrival(Instant scheduledArrival) {
        this.scheduledArrival = scheduledArrival;
    }

    public Instant getScheduledDeparture() {
        return scheduledDeparture;
    }

    public void setScheduledDeparture(Instant scheduledDeparture) {
        this.scheduledDeparture = scheduledDeparture;
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

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public List<TripStopPrediction> getStopPredictions() {
        return stopPredictions;
    }

    public void setStopPredictions(List<TripStopPrediction> stopPredictions) {
        this.stopPredictions.clear();
        if (stopPredictions != null) {
            for (TripStopPrediction prediction : stopPredictions) {
                prediction.setTripState(this);
                prediction.setTripId(this.getTripId());
                this.stopPredictions.add(prediction);
            }
        }
    }
}
