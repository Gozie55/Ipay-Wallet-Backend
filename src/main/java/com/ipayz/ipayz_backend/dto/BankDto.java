package com.ipayz.ipayz_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BankDto {
    private String code;
    private String name;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
