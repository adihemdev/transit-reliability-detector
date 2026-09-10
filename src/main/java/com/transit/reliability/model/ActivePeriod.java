package com.transit.reliability.model;

import java.time.Instant;

public record ActivePeriod(
        Instant start,
        Instant end
) {}
