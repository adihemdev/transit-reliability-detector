package com.transit.reliability.model;

import java.io.Serializable;
import java.util.Objects;

public class TripStopPredictionId implements Serializable {

    private String tripId;
    private Integer stopSequence;

    // Constructors
    public TripStopPredictionId() {
    }

    public TripStopPredictionId(String tripId, Integer stopSequence) {
        this.tripId = tripId;
        this.stopSequence = stopSequence;
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

    // Equals & HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TripStopPredictionId that = (TripStopPredictionId) o;
        return Objects.equals(tripId, that.tripId) &&
                Objects.equals(stopSequence, that.stopSequence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tripId, stopSequence);
    }
}
