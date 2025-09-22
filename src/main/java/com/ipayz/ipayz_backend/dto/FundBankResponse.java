package com.ipayz.ipayz_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FundBankResponse {
    private String bankName;
    private String accountNumber;
    private String reference;

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
