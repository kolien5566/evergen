package com.neovolt.evergen.model.queue;

import java.util.List;

import com.neovolt.evergen.model.site.BatteryInverter;
import com.neovolt.evergen.model.site.HybridInverter;
import com.neovolt.evergen.model.site.Meter;
import com.neovolt.evergen.model.site.SolarInverter;

import lombok.Data;

@Data
public class TelemetryData {
    private String siteId;
    private List<BatteryInverter> batteryInverters;
    private List<HybridInverter> hybridInverters;
    private List<SolarInverter> solarInverters;
    private List<Meter> meters;
}
