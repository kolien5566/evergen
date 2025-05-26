package com.neovolt.evergen.model.bytewatt;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SecondLevelData {
    // 储能系统S/N
    @JsonProperty("sys_sn")
    private String sysSn;
    
    // 数据上传时间
    @JsonProperty("uploadtime")
    private LocalDateTime uploadtime;
    
    // PV输入功率1
    @JsonProperty("ppv1")
    private Double ppv1;
    
    // PV输入功率2
    @JsonProperty("ppv2")
    private Double ppv2;
    
    // PV输入功率3
    @JsonProperty("ppv3")
    private Double ppv3;
    
    // PV输入功率4
    @JsonProperty("ppv4")
    private Double ppv4;
    
    // 逆变器L1实时输出功率，该参数有正负
    @JsonProperty("preal_l1")
    private Double prealL1;
    
    // 逆变器L2实时输出功率，该参数有正负
    @JsonProperty("preal_l2")
    private Double prealL2;
    
    // 逆变器L3实时输出功率，该参数有正负
    @JsonProperty("preal_l3")
    private Double prealL3;
    
    // 电表L1实时功率
    @JsonProperty("pmeter_l1")
    private Double pmeterL1;
    
    // 电表L2实时功率
    @JsonProperty("pmeter_l2")
    private Double pmeterL2;
    
    // 电表L3实时功率
    @JsonProperty("pmeter_l3")
    private Double pmeterL3;
    
    // 电表的实时功率
    @JsonProperty("pmeter_dc")
    private Double pmeterDc;
    
    // 电池实时功率
    @JsonProperty("pbat")
    private Double pbat;
    
    // 逆变器视在功率
    @JsonProperty("sva")
    private Double sva;
    
    // 入网电表无功功率
    @JsonProperty("varac")
    private Double varac;
    
    // 并网逆变器侧电表无功功率
    @JsonProperty("vardc")
    private Double vardc;
    
    // 电池Soc
    @JsonProperty("soc")
    private Double soc;
}
