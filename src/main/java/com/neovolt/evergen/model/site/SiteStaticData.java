package com.neovolt.evergen.model.site;

import lombok.Data;
import java.util.List;

@Data
public class SiteStaticData {
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
        private String deviceId;
        private String manufacturer;
        private String model;
        private MeterType type;
        private String location;

        public enum MeterType {
            GRID("grid"),
            LOAD("load"),
            GENERATION("generation");

            private final String value;

            MeterType(String value) {
                this.value = value;
            }
        }
    }

    @Data
    public static class PvString {
        private String id;
        private Integer nominalPowerW;
        private Double azimuthDeg;
        private Double inclinationDeg;
    }
}
