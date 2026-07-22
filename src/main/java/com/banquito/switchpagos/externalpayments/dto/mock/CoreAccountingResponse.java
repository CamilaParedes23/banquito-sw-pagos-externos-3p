package com.banquito.switchpagos.externalpayments.dto.mock;

public record CoreAccountingResponse(
        boolean success,
        String coreTransactionId,
        String code,
        String message) {
}
