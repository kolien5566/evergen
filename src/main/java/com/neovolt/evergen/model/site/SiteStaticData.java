package com.neovolt.evergen.model.site;

import java.util.List;

import lombok.Data;

@Data
public class SiteStaticData {
    private String siteId;
    private List<BatteryInverterStatic> batteryInverters;
    private List<HybridInverterStatic> hybridInverters;
    private List<SolarInverterStatic> solarInverters;
    private List<MeterStatic> meters;

    @Data
    public static class BatteryInverterStatic {
        private String deviceId;
        private String manufacturer;
        private String model;
        private Integer nominalPowerW;
        private Integer nominalCapacityWh;
        private Integer maxChargePowerW;
        private Integer maxDischargePowerW;
    }

    @Data
    public static class HybridInverterStatic {
        private String deviceId;
        private String manufacturer;
        private String model;
        private Integer nominalPowerW;
        private Integer nominalCapacityWh;
        private Integer maxChargePowerW;
        private Integer maxDischargePowerW;
        private List<String> connectedPvStrings;
    }

    @Data
    public static class SolarInverterStatic {
        private String deviceId;
        private String manufacturer;
        private String model;
        private Integer nominalPowerW;
        private List<String> connectedPvStrings;
    }

    @Data
    public static class MeterStatic {
        // 常量定义，提供类型安全的引用方式
        public static final String METER_TYPE_GRID = "grid";
        public static final String METER_TYPE_LOAD = "load";
        public static final String METER_TYPE_GENERATION = "generation";
        
        private String deviceId;
        private String manufacturer;
        private String model;
        private String type;
        private String location;
    }

    @Data
    public static class PvString {
        private String id;
        private Integer nominalPowerW;
        private Double azimuthDeg;
        private Double inclinationDeg;
    }
}
