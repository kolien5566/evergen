package com.neovolt.evergen.model.site;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class Meter {
    private String deviceId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime deviceTime;

    private Integer powerW;
    private Integer reactivePowerVar;

    private Double gridVoltage1V;
    private Double gridVoltage2V;
    private Double gridVoltage3V;
    private Double gridFrequencyHz;

    private Double cumulativeGridImportWh;
    private Double cumulativeGridExportWh;
}
