package com.neovolt.evergen.model.cloudevent;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

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
