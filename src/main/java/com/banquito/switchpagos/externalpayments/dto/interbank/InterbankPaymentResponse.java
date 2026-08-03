package com.banquito.switchpagos.externalpayments.dto.interbank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InterbankPaymentResponse(
        UUID interbankTransferUuid,
        UUID sourceTransferUuid,
        UUID paymentLineUuid,
        UUID batchUuid,
        String direction,
        String status,
        String sourceRoutingCode,
        String destinationRoutingCode,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency,
        UUID destinationTransactionUuid,
        UUID journalEntryUuid,
        UUID reversalTransactionUuid,
        UUID reversalJournalEntryUuid,
        String receiptNumber,
        String errorCode,
        String message,
        String accountingDate,
        OffsetDateTime processedAt,
        Boolean idempotencyReplayed,
        UUID correlationId) {
}
