package com.neovolt.evergen.model.queue;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class CloudEventData {
    private String specversion = "1.0";
    private String type;
    private String source;
    private String id;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime time;

    private String datacontenttype = "application/json";
    private Object data;
}
