package com.transit.reliability.model;

public record AffectedEntity(
        String routeId,
        String stopId
) {}
