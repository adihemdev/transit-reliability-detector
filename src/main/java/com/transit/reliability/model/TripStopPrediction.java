package com.transit.reliability.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "trip_stop_predictions")
@IdClass(TripStopPredictionId.class)
public class TripStopPrediction {

    @Id
    @Column(name = "trip_id")
    private String tripId;

    @Id
    @Column(name = "stop_sequence")
    private Integer stopSequence;

    @Column(name = "stop_id", nullable = false)
    private String stopId;

    @Column(name = "predicted_arrival")
    private Instant predictedArrival;

    @Column(name = "predicted_departure")
    private Instant predictedDeparture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", referencedColumnName = "trip_id", insertable = false, updatable = false)
    @JsonIgnore
    private TripState tripState;

    // Constructors
    public TripStopPrediction() {
    }

    public TripStopPrediction(String tripId, Integer stopSequence, String stopId,
                              Instant predictedArrival, Instant predictedDeparture) {
        this.tripId = tripId;
        this.stopSequence = stopSequence;
        this.stopId = stopId;
        this.predictedArrival = predictedArrival;
        this.predictedDeparture = predictedDeparture;
    }

    // Getters and Setters
    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public Integer getStopSequence() {
        return stopSequence;
    }

    public void setStopSequence(Integer stopSequence) {
        this.stopSequence = stopSequence;
    }

    public String getStopId() {
        return stopId;
    }

    public void setStopId(String stopId) {
        this.stopId = stopId;
    }

    public Instant getPredictedArrival() {
        return predictedArrival;
    }

    public void setPredictedArrival(Instant predictedArrival) {
        this.predictedArrival = predictedArrival;
    }

    public Instant getPredictedDeparture() {
        return predictedDeparture;
    }

    public void setPredictedDeparture(Instant predictedDeparture) {
        this.predictedDeparture = predictedDeparture;
    }

    public TripState getTripState() {
        return tripState;
    }

    public void setTripState(TripState tripState) {
        this.tripState = tripState;
    }
}
