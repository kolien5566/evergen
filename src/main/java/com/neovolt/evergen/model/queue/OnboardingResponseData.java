package com.neovolt.evergen.model.queue;

import com.neovolt.evergen.model.site.SiteStaticData;

import lombok.Data;

@Data
public class OnboardingResponseData {
    // 连接状态常量
    public static final String CONNECTION_STATUS_CONNECTED = "connected";
    public static final String CONNECTION_STATUS_NOT_CONNECTED = "not-connected";
    
    // 错误原因常量
    public static final String ERROR_REASON_WRONG_SERIAL = "wrong-serial";
    public static final String ERROR_REASON_REGISTRATION_INCOMPLETE = "registration-incomplete";
    public static final String ERROR_REASON_IN_OTHER_VPP = "in-other-vpp";
    public static final String ERROR_REASON_DEVICE_OFFLINE = "device-offline";
    public static final String ERROR_REASON_INCOMPATIBLE_HARDWARE = "incompatible-hardware";
    public static final String ERROR_REASON_INCOMPATIBLE_FIRMWARE = "incompatible-firmware";
    
    private String serialNumber;
    private String deviceId;
    private String connectionStatus;
    private String errorReason;
    private SiteStaticData siteStaticData;
}
