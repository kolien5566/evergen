package com.neovolt.evergen.model.cloudevent;

import lombok.Data;

@Data
public class OffboardingResponseData {
    // 常量定义，提供类型安全的引用方式
    public static final String CONNECTION_STATUS_CONNECTED = "connected";
    public static final String CONNECTION_STATUS_NOT_CONNECTED = "not-connected";
    
    public static final String ERROR_REASON_WRONG_SERIAL = "wrong-serial";
    public static final String ERROR_REASON_DEVICE_OFFLINE = "device-offline";
    
    private String serialNumber;
    private String deviceId;
    private String connectionStatus;
    private String errorReason;
}
