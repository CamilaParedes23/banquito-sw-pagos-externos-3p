package com.banquito.switchpagos.externalpayments.client;

public class ExternalBankTimeoutException extends ExternalBankClientException {
    public ExternalBankTimeoutException(String message) {
        super("EXTERNAL_BANK_TIMEOUT", message);
    }
}
