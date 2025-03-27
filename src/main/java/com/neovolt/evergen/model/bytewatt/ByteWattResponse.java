package com.neovolt.evergen.model.bytewatt;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ByteWattResponse<T> {
    @JsonProperty("code")
    private int code;

    @JsonProperty("info")
    private String info;

    @JsonProperty("data")
    private T data;
}
