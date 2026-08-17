package com.transit.reliability.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a timing prediction update at a specific stop for a trip.
 */
public class StopTimingUpdate {

    private String stopId;
    private Instant scheduledArrival;
    private Instant predictedArrival;
    private Instant scheduledDeparture;
    private Instant predictedDeparture;

    // Default constructor for Jackson
    public StopTimingUpdate() {
    }

    // All-args constructor
    public StopTimingUpdate(String stopId, Instant scheduledArrival, Instant predictedArrival,
                            Instant scheduledDeparture, Instant predictedDeparture) {
        this.stopId = stopId;
        this.scheduledArrival = scheduledArrival;
        this.predictedArrival = predictedArrival;
        this.scheduledDeparture = scheduledDeparture;
        this.predictedDeparture = predictedDeparture;
    }

    // Getters and Setters
    public String getStopId() {
        return stopId;
    }

    public void setStopId(String stopId) {
        this.stopId = stopId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StopTimingUpdate that = (StopTimingUpdate) o;
        return Objects.equals(stopId, that.stopId) &&
                Objects.equals(scheduledArrival, that.scheduledArrival) &&
                Objects.equals(predictedArrival, that.predictedArrival) &&
                Objects.equals(scheduledDeparture, that.scheduledDeparture) &&
                Objects.equals(predictedDeparture, that.predictedDeparture);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stopId, scheduledArrival, predictedArrival, scheduledDeparture, predictedDeparture);
    }

    @Override
    public String toString() {
        return "StopTimingUpdate{" +
                "stopId='" + stopId + '\'' +
                ", scheduledArrival=" + scheduledArrival +
                ", predictedArrival=" + predictedArrival +
                ", scheduledDeparture=" + scheduledDeparture +
                ", predictedDeparture=" + predictedDeparture +
                '}';
    }
}
