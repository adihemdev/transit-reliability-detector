package com.transit.reliability.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class TripUpdateId implements Serializable {

    private String tripId;
    private Instant eventTimestamp;

    // Constructors
    public TripUpdateId() {
    }

    public TripUpdateId(String tripId, Instant eventTimestamp) {
        this.tripId = tripId;
        this.eventTimestamp = eventTimestamp;
    }

    // Getters and Setters
    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    // Equals & HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TripUpdateId that = (TripUpdateId) o;
        return Objects.equals(tripId, that.tripId) &&
                Objects.equals(eventTimestamp, that.eventTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tripId, eventTimestamp);
    }
}
