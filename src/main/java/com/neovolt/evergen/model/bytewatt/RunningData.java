package com.neovolt.evergen.model.bytewatt;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class RunningData {
    //储能机（混网）SN
    @JsonProperty("sys_sn")
    private String sysSn;
    //数据上传时间
    @JsonProperty("upload_datetime")
    private LocalDateTime uploadDatetime;

    // 四路光伏发电输入
    // PV输入功率1
    @JsonProperty("p_pv1")
    private Double pPv1;
    // PV输入功率2
    @JsonProperty("p_pv2")
    private Double pPv2;
    // PV输入功率3
    @JsonProperty("p_pv3")
    private Double pPv3;
    // PV输入功率4
    @JsonProperty("p_pv4")
    private Double pPv4;

    // 三相电网
    // L1市电电压
    @JsonProperty("u_a")
    private Double uA;
    // L2市电电压
    @JsonProperty("u_b")
    private Double uB;
    // L3市电电压
    @JsonProperty("u_c")
    private Double uC;

    // 电网频率
    @JsonProperty("fac")
    private Double fac;

    // 电池电量
    @JsonProperty("soc")
    private Double soc;

    // 电池电压
    @JsonProperty("bat_v")
    private Double batV;

    // 电池电流
    @JsonProperty("bat_c")
    private Double batC;

    // 逆变器工作模式
    @JsonProperty("inv_work_mode")
    private Integer invWorkMode;

    // PV总输入能量
    @JsonProperty("epv_total")
    private Double epvTotal;
    
    // 电表入户能量
    @JsonProperty("e_input")
    private Double eInput;

    // 电表出户（馈网）能量    
    @JsonProperty("e_output")
    private Double eOutput;

    // 电池充电能量
    @JsonProperty("e_charge")
    private Double eCharge;

    // 电表L1功率
    @JsonProperty("p_meter_l1")
    private Double pMeterL1;
    // 电表L2功率
    @JsonProperty("p_meter_l2")
    private Double pMeterL2;
    // 电表L3功率
    @JsonProperty("p_meter_l3")
    private Double pMeterL3;
    // 光伏并网机的电表功率
    @JsonProperty("p_meter_dc")
    private Double pMeterDc;

    // 电池功率
    @JsonProperty("p_bat")
    private Double pBat;
}
