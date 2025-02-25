package com.neovolt.evergen.model.cloudevent;

import lombok.Data;

@Data
public class OffboardingResponseData {
    private String serialNumber;
    private String deviceId;
    private ConnectionStatus connectionStatus;
    private String errorReason;

    public enum ConnectionStatus {
        CONNECTED("connected"),
        NOT_CONNECTED("not-connected");

        private final String value;

        ConnectionStatus(String value) {
            this.value = value;
        }
    }
}
