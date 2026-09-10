package com.transit.reliability.model;

import java.time.Instant;
import java.util.List;

public record ServiceAlertEvent(
        String alertId,
        List<ActivePeriod> activePeriods,
        List<AffectedEntity> affectedEntities,
        String headerText,
        Instant createdAt,
        Instant updatedAt,
        String alertType
) {}