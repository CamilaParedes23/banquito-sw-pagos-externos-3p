package com.banquito.switchpagos.externalpayments.dto.mock;

import java.time.OffsetDateTime;

public record ExternalPaymentResponse(
        String externalPaymentId,
        String status,
        String failureCode,
        String message,
        OffsetDateTime processedAt) {
}
