package com.neovolt.evergen.model.queue;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class CommandData {
    private String deviceId;
    private RealMode realMode;
    private ReactiveMode reactiveMode;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime startTime;
    private Integer durationSeconds;

    @Data
    public static class RealMode {
        private SelfConsumptionCommand selfConsumptionCommand;
        private ChargeOnlySelfConsumptionCommand chargeOnlySelfConsumptionCommand;
        private ChargeCommand chargeCommand;
        private DischargeCommand dischargeCommand;
    }

    @Data
    public static class ReactiveMode {
        private PowerFactorCorrection powerFactorCorrection;
        private ReactiveInject inject;
        private ReactiveAbsorb absorb;
    }

    @Data
    public static class PowerFactorCorrection {
        private Double targetPowerFactor;
    }

    @Data
    public static class ReactiveInject {
        private Integer reactivePowerVar;
    }

    @Data
    public static class ReactiveAbsorb {
        private Integer reactivePowerVar;
    }

    @Data
    public static class ChargeCommand {
        private Integer powerW;
    }

    @Data
    public static class DischargeCommand {
        private Integer powerW;
    }

    @Data
    public static class SelfConsumptionCommand {
        // Empty class as per spec
    }

    @Data
    public static class ChargeOnlySelfConsumptionCommand {
        // Empty class as per spec
    }
}
