package com.transit.reliability.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.transit.reliability.model.ActivePeriod;
import com.transit.reliability.model.AffectedEntity;
import com.transit.reliability.model.ServiceAlertEvent;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class MtaServiceAlertMapper {

    public ServiceAlertEvent map(JsonNode entity) {

        String alertId = entity.get("id").asText();

        JsonNode alertNode = entity.get("alert");

        String headerText = alertNode
                .get("header_text")
                .get("translation")
                .get(0)
                .get("text")
                .asText();

        JsonNode mercuryAlertNode =
                alertNode.get("transit_realtime.mercury_alert");

        String alertType = mercuryAlertNode.get("alert_type").asText();

        // Map active periods
        JsonNode activePeriodsNode = alertNode.get("active_period");
        List<ActivePeriod> activePeriods = new ArrayList<>();

        for (JsonNode periodNode : activePeriodsNode) {
            Instant start = periodNode.has("start")
                    ? Instant.ofEpochSecond(periodNode.get("start").asLong())
                    : null;

            Instant end = periodNode.has("end")
                    ? Instant.ofEpochSecond(periodNode.get("end").asLong())
                    : null;

            activePeriods.add(new ActivePeriod(start, end));
        }

        // Map affected entities
        JsonNode affectedEntitiesNode = alertNode.get("informed_entity");
        List<AffectedEntity> affectedEntities = new ArrayList<>();

        for (JsonNode entityNode : affectedEntitiesNode) {
            String routeId = entityNode.has("route_id")
                    ? entityNode.get("route_id").asText()
                    : null;

            String stopId = entityNode.has("stop_id")
                    ? entityNode.get("stop_id").asText()
                    : null;

            affectedEntities.add(new AffectedEntity(routeId, stopId));
        }

        // Map created and updated timestamps
        Instant createdAt = mercuryAlertNode.has("created_at")
                ? Instant.ofEpochSecond(mercuryAlertNode.get("created_at").asLong())
                : null;

        Instant updatedAt = mercuryAlertNode.has("updated_at")
                ? Instant.ofEpochSecond(mercuryAlertNode.get("updated_at").asLong())
                : null;

        return new ServiceAlertEvent(
                alertId,
                activePeriods,
                affectedEntities,
                headerText,
                createdAt,
                updatedAt,
                alertType
        );
    }
}