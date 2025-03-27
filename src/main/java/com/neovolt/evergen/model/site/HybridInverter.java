package com.neovolt.evergen.model.site;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class HybridInverter {
    private String deviceId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime deviceTime;

    private Integer batteryPowerW;
    private Integer meterPowerW;
    private Integer solarPowerW;
    private Integer batteryReactivePowerVar;
    private Integer meterReactivePowerVar;

    private Double gridVoltage1V;
    private Double gridVoltage2V;
    private Double gridVoltage3V;
    private Double gridFrequencyHz;

    private Double cumulativeBatteryChargeEnergyWh;
    private Double cumulativeBatteryDischargeEnergyWh;
    private Double cumulativePvGenerationWh;
    private Double cumulativeGridImportWh;
    private Double cumulativeGridExportWh;

    private Double stateOfCharge;
    private Double stateOfHealth;
    private Integer maxChargePowerW;
    private Integer maxDischargePowerW;
}
