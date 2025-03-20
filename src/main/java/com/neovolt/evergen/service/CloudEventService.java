package com.neovolt.evergen.service;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.format.EventSerializationException;
import io.cloudevents.jackson.JsonFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * CloudEvent服务，提供创建和序列化/反序列化CloudEvent的功能
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CloudEventService {

    private final ObjectMapper objectMapper;
    private final JsonFormat jsonFormat = new JsonFormat();

    /**
     * 创建CloudEvent
     *
     * @param type 事件类型
     * @param source 事件来源
     * @param data 事件数据
     * @return CloudEvent实例
     */
    public <T> CloudEvent createCloudEvent(String type, String source, T data) {
        try {
            byte[] jsonData = objectMapper.writeValueAsBytes(data);
            
            return CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withType(type)
                    .withSource(URI.create(source))
                    .withTime(OffsetDateTime.now())
                    .withDataContentType("application/json")
                    .withData(jsonData)
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Error serializing data to CloudEvent: {}", e.getMessage());
            throw new RuntimeException("Failed to create CloudEvent", e);
        }
    }

    /**
     * 将CloudEvent序列化为JSON字节数组
     *
     * @param event CloudEvent实例
     * @return JSON字节数组
     */
    public byte[] serialize(CloudEvent event) {
        try {
            return jsonFormat.serialize(event);
        } catch (EventSerializationException e) {
            log.error("Error serializing CloudEvent: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize CloudEvent", e);
        }
    }

    /**
     * 将CloudEvent序列化为JSON字符串
     *
     * @param event CloudEvent实例
     * @return JSON字符串
     */
    public String serializeToString(CloudEvent event) {
        return new String(serialize(event));
    }

    /**
     * 从JSON字节数组反序列化CloudEvent
     *
     * @param bytes JSON字节数组
     * @return CloudEvent实例
     */
    public CloudEvent deserialize(byte[] bytes) {
        try {
            return jsonFormat.deserialize(bytes);
        } catch (Exception e) {
            log.error("Error deserializing CloudEvent: {}", e.getMessage());
            throw new RuntimeException("Failed to deserialize CloudEvent", e);
        }
    }

    /**
     * 从JSON字符串反序列化CloudEvent
     *
     * @param json JSON字符串
     * @return CloudEvent实例
     */
    public CloudEvent deserializeFromString(String json) {
        return deserialize(json.getBytes());
    }

    /**
     * 提取CloudEvent中的数据并转换为指定类型
     *
     * @param event CloudEvent实例
     * @param clazz 目标类型
     * @return 转换后的数据对象
     */
    public <T> T extractData(CloudEvent event, Class<T> clazz) {
        try {
            if (event.getData() == null) {
                return null;
            }
            return objectMapper.readValue(event.getData().toBytes(), clazz);
        } catch (Exception e) {
            log.error("Error deserializing CloudEvent data: {}", e.getMessage());
            throw new RuntimeException("Failed to extract data from CloudEvent", e);
        }
    }
}