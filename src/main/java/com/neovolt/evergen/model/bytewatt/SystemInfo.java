package com.neovolt.evergen.model.bytewatt;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SystemInfo {
    // 储能系统SN
    @JsonProperty("sys_sn")
    private String sysSn;
    // 系统型号
    @JsonProperty("system_model")
    private String systemModel;
    // 电池容量
    @JsonProperty("cobat")
    private Double cobat;
    // 电池可用容量
    @JsonProperty("usable_capacity")
    private Double usableCapacity;
    // 电池型号
    @JsonProperty("mbat")
    private String mbat;
    // 逆变器额定输出功率
    @JsonProperty("poinv")
    private Double poinv;
    // 额定PV装机容量
    @JsonProperty("popv")
    private Double popv;
    // 系统接线模式
    @JsonProperty("solution")
    private String solution;
    // EMS版本
    @JsonProperty("ems_version")
    private String emsVersion;
    // BMS版本
    @JsonProperty("bms_version")
    private String bmsVersion;
    // 逆变器版本
    @JsonProperty("inv_version")
    private String invVersion;
    // 逆变器型号
    @JsonProperty("inv_model")
    private String invModel;
    // 电表型号
    @JsonProperty("meter_model")
    private String meterModel;
    // 电表相位
    @JsonProperty("meter_phase")
    private Integer meterPhase;
    // 最大馈网功率系数
    @JsonProperty("set_feed")
    private Integer setFeed;
    // 设备是否在线（1：online，0：offline）
    @JsonProperty("net_work_status")
    private Integer netWorkStatus;
    // 系统状态
    @JsonProperty("state")
    private String state;
    // 数据传输频率(10秒,300秒)
    @JsonProperty("trans_frequency")
    private Integer transFrequency;
    // 坐标纬度
    @JsonProperty("latitude")
    private String latitude;
    // 坐标经度
    @JsonProperty("longitude")
    private String longitude;
    // 时区, 这边api会传这样的数据 "timezone":"+10:00"
    @JsonProperty("timezone")
    private String timezone;
    // 安规ID
    @JsonProperty("safe")
    private Integer safe;
    // 系统名称备注
    @JsonProperty("remark")
    private String remark;
}
