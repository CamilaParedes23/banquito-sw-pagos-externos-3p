package com.banquito.switchpagos.externalpayments.client;

public class ExternalBankConflictException extends ExternalBankClientException {
    public ExternalBankConflictException(String message) {
        super("IDEMPOTENCY_CONFLICT", message, 409);
    }
}
