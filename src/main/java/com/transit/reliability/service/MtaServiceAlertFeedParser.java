package com.transit.reliability.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transit.reliability.model.ServiceAlertEvent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MtaServiceAlertFeedParser {

    private final ObjectMapper objectMapper;
    private final MtaServiceAlertMapper mapper;

    public MtaServiceAlertFeedParser(ObjectMapper objectMapper,
                                     MtaServiceAlertMapper mapper) {
        this.objectMapper = objectMapper;
        this.mapper = mapper;
    }

    public List<ServiceAlertEvent> parse(String json) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode entities = root.path("entity");

        List<ServiceAlertEvent> alerts = new ArrayList<>();

        for (JsonNode entity : entities) {
            if (entity.has("alert")) {
                alerts.add(mapper.map(entity));
            }
        }

        return alerts;
    }
}