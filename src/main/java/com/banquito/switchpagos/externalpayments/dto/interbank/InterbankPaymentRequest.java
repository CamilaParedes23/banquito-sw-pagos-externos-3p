package com.banquito.switchpagos.externalpayments.dto.interbank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InterbankPaymentRequest(
        UUID sourceTransferUuid,
        UUID paymentLineUuid,
        UUID batchUuid,
        String sourceRoutingCode,
        String destinationRoutingCode,
        String sourceAccountNumber,
        String destinationAccountNumber,
        String originatorIdentification,
        String originatorName,
        String beneficiaryIdentification,
        String beneficiaryName,
        String beneficiaryEmail,
        String concept,
        BigDecimal amount,
        String currency,
        LocalDate accountingDate,
        UUID correlationId) {
}
