package com.banquito.switchpagos.externalpayments.client;

public class ExternalBankClientException extends RuntimeException {
    private final String code;
    private final Integer httpStatus;

    public ExternalBankClientException(String code, String message) {
        this(code, message, null);
    }

    public ExternalBankClientException(String code, String message, Integer httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }
}
