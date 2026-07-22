package com.banquito.switchpagos.externalpayments.client;

public class ExternalBankClientException extends RuntimeException {
    private final String code;

    public ExternalBankClientException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
