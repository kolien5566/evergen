package com.neovolt.evergen.model.site;

import java.time.Instant;
import java.util.List;

import lombok.Data;

@Data
public class SiteStaticData {
    private String siteId;
    private String uniqueMeterIdentifier;
    private String country;
    private String distributionNetworkOperator;
    private String state;
    private String postcode;
    private String address;
    private Integer exportLimitW;

    private List<BatteryStaticData> batteriesStaticData;
    private List<BatteryInverterStaticData> batteryInvertersStaticData;
    private List<HybridInverterStaticData> hybridInvertersStaticData;
    private List<SolarInverterStaticData> solarInvertersStaticData;
    private List<MeterStaticData> metersStaticData;

    @Data
    public static class BatteryStaticData {
        private String deviceId;
        private String serialNumber;
        private String manufacturer;
        private String model;
        private String firmware;
        private Integer nameplateEnergyCapacityWh;
        private Integer maxChargePowerW;
        private Integer maxDischargePowerW;
        private Integer cumulativeBatteryChargeEnergyWh;
        private Integer cumulativeBatteryDischargeEnergyWh;
    }

    @Data
    public static class BatteryInverterStaticData {
        private String deviceId;
        private String serialNumber;
        private String manufacturer;
        private String model;
        private String firmware;
        private Instant installationDate;
        private List<String> connectedBatteryIds;
        private Integer batteryInverterAcCapacityW;
        private Integer solarInverterAcCapacityW;
    }

    @Data
    public static class HybridInverterStaticData {
        private String deviceId;
        private String serialNumber;
        private String manufacturer;
        private String model;
        private String firmware;
        private Instant installationDate;
        private Integer hybridInverterAcCapacityW;
        private Integer solarInverterAcCapacityW;
        private Integer solarArrayRatedDcOutputW;
        private List<String> connectedBatteryIds;
    }

    @Data
    public static class SolarInverterStaticData {
        private String deviceId;
        private String serialNumber;
        private String manufacturer;
        private String model;
        private String firmware;
        private Instant installationDate;
        private Integer solarInverterAcCapacityW;
        private Integer solarArrayRatedDcOutputW;
    }

    @Data
    public static class MeterStaticData {
        private String deviceId;
        private String serialNumber;
        private String manufacturer;
        private String model;
        private String firmware;
        private Boolean hasControllableLoad;
        private Integer phase;
    }
}
