package com.neovolt.evergen.model.device;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SolarInverter {
    private String deviceId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime deviceTime;

    private Integer powerW;
    private Integer reactivePowerVar;

    private Double gridVoltage1V;
    private Double gridVoltage2V;
    private Double gridVoltage3V;
    private Double gridFrequencyHz;

    private Double cumulativeGenerationWh;
}
