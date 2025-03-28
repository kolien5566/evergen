package com.neovolt.evergen.model.bytewatt;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * 系统列表响应数据结构
 * 用于解析GetSystemList API的响应
 */
@Data
public class SystemListResponse {
    @JsonProperty("total_count")
    private int totalCount;
    
    @JsonProperty("total_page_count")
    private int totalPageCount;
    
    @JsonProperty("page_index")
    private int pageIndex;
    
    @JsonProperty("page_size")
    private int pageSize;
    
    @JsonProperty("systems")
    private List<SystemInfo> systems;
}
