package com.neovolt.evergen.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovolt.evergen.model.cloudevent.CloudEventData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudEventService {

    private final ObjectMapper objectMapper;

    public CloudEventData createCloudEvent(String type, String source, Object data) {
        CloudEventData cloudEvent = new CloudEventData();
        cloudEvent.setType(type);
        cloudEvent.setSource(source);
        cloudEvent.setId(UUID.randomUUID().toString());
        cloudEvent.setTime(LocalDateTime.now());
        cloudEvent.setData(data);
        return cloudEvent;
    }

    public String serializeCloudEvent(CloudEventData cloudEvent) {
        try {
            return objectMapper.writeValueAsString(cloudEvent);
        } catch (Exception e) {
            log.error("Failed to serialize CloudEvent: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize CloudEvent", e);
        }
    }

    public CloudEventData deserializeCloudEvent(String message) {
        try {
            return objectMapper.readValue(message, CloudEventData.class);
        } catch (Exception e) {
            log.error("Failed to deserialize CloudEvent: {}", e.getMessage());
            throw new RuntimeException("Failed to deserialize CloudEvent", e);
        }
    }
}
