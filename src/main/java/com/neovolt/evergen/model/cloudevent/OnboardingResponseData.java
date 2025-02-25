package com.neovolt.evergen.model.cloudevent;

import com.neovolt.evergen.model.site.SiteStaticData;
import lombok.Data;

@Data
public class OnboardingResponseData {
    private String serialNumber;
    private String deviceId;
    private ConnectionStatus connectionStatus;
    private ErrorReason errorReason;
    private SiteStaticData siteStaticData;

    public enum ConnectionStatus {
        CONNECTED("connected"),
        NOT_CONNECTED("not-connected");

        private final String value;

        ConnectionStatus(String value) {
            this.value = value;
        }
    }

    public enum ErrorReason {
        WRONG_SERIAL("wrong-serial"),
        REGISTRATION_INCOMPLETE("registration-incomplete"),
        IN_OTHER_VPP("in-other-vpp"),
        DEVICE_OFFLINE("device-offline"),
        INCOMPATIBLE_HARDWARE("incompatible-hardware"),
        INCOMPATIBLE_FIRMWARE("incompatible-firmware");

        private final String value;

        ErrorReason(String value) {
            this.value = value;
        }
    }
}
