package com.banquito.switchpagos.externalpayments.dto.mock;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CoreAccountingRequest(
        UUID batchId,
        UUID lineId,
        String coreFundingId,
        String externalPaymentId,
        String sourceAccountNumber,
        String destinationAccountNumber,
        String routingCode,
        BigDecimal amount,
        String currency,
        OffsetDateTime processedAt,
        String idempotencyKey,
        String reference) {
}
