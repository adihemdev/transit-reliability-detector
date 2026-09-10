package com.transit.reliability.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transit.reliability.model.ServiceAlertEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MtaServiceAlertMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MtaServiceAlertMapper mapper = new MtaServiceAlertMapper();

    @Test
    void mapsServiceAlertWithMultipleAffectedStops() throws Exception {

        String json = """
        {
          "id": "lmm:planned_work:33094",
          "alert": {
            "active_period": [
              {
                "start": 1787024847,
                "end": 1787027700
              }
            ],
            "informed_entity": [
              {
                "agency_id": "MTASBWY",
                "route_id": "N",
                "stop_id": "R11"
              },
              {
                "agency_id": "MTASBWY",
                "route_id": "N",
                "stop_id": "R13"
              }
            ],
            "header_text": {
              "translation": [
                {
                  "text": "No [N] trains between stations"
                }
              ]
            },
            "transit_realtime.mercury_alert": {
              "created_at": 1787024847,
              "updated_at": 1787026115,
              "alert_type": "Planned Work"
            }
          }
        }
        """;

        JsonNode entity = objectMapper.readTree(json);

        ServiceAlertEvent event = mapper.map(entity);

        assertEquals("lmm:planned_work:33094", event.alertId());
        assertEquals("Planned Work", event.alertType());
        assertEquals("No [N] trains between stations", event.headerText());

        assertEquals(
                Instant.ofEpochSecond(1787024847),
                event.createdAt()
        );

        assertEquals(
                Instant.ofEpochSecond(1787026115),
                event.updatedAt()
        );

        assertEquals(1, event.activePeriods().size());

        assertEquals(
                Instant.ofEpochSecond(1787024847),
                event.activePeriods().get(0).start()
        );

        assertEquals(
                Instant.ofEpochSecond(1787027700),
                event.activePeriods().get(0).end()
        );

        assertEquals(2, event.affectedEntities().size());

        assertEquals(
                "N",
                event.affectedEntities().get(0).routeId()
        );

        assertEquals(
                "R11",
                event.affectedEntities().get(0).stopId()
        );

        assertEquals(
                "N",
                event.affectedEntities().get(1).routeId()
        );

        assertEquals(
                "R13",
                event.affectedEntities().get(1).stopId()
        );
    }

    @Test
    void mapsRouteOnlyAlertWithoutStopId() throws Exception {

        String json = """
        {
          "id": "lmm:planned_work:29070",
          "alert": {
            "active_period": [
              {
                "start": 1787024847,
                "end": 1787027700
              }
            ],
            "informed_entity": [
              {
                "agency_id": "MTASBWY",
                "route_id": "L"
              }
            ],
            "header_text": {
              "translation": [
                {
                  "text": "In Manhattan, all [L] trains at 3 Av and 1 Av board from the 8 Av-bound platform"
                }
              ]
            },
            "transit_realtime.mercury_alert": {
              "created_at": 1787024847,
              "updated_at": 1787026115,
              "alert_type": "Planned Work"
            }
          }
        }
        """;

        JsonNode entity = objectMapper.readTree(json);

        ServiceAlertEvent event = mapper.map(entity);

        assertEquals("lmm:planned_work:29070", event.alertId());

        assertEquals(1, event.affectedEntities().size());

        assertEquals(
                "L",
                event.affectedEntities().get(0).routeId()
        );

        assertNull(
                event.affectedEntities().get(0).stopId()
        );
    }
}
