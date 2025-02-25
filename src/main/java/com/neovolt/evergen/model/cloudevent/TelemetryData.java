package com.neovolt.evergen.model.cloudevent;

import com.neovolt.evergen.model.device.BatteryInverter;
import com.neovolt.evergen.model.device.HybridInverter;
import com.neovolt.evergen.model.device.Meter;
import com.neovolt.evergen.model.device.SolarInverter;
import lombok.Data;

import java.util.List;

@Data
public class TelemetryData {
    private String siteId;
    private List<BatteryInverter> batteryInverters;
    private List<HybridInverter> hybridInverters;
    private List<SolarInverter> solarInverters;
    private List<Meter> meters;
}
