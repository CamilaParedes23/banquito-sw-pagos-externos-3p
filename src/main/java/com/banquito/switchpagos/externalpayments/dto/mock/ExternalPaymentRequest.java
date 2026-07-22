package com.banquito.switchpagos.externalpayments.dto.mock;

import java.math.BigDecimal;
import java.util.UUID;

public record ExternalPaymentRequest(
        UUID batchId,
        UUID lineId,
        UUID correlationId,
        String originBankCode,
        String destinationBankCode,
        String sourceAccountNumber,
        String destinationAccountNumber,
        String beneficiaryIdentification,
        String beneficiaryName,
        BigDecimal amount,
        String currency,
        String reference) {
}
