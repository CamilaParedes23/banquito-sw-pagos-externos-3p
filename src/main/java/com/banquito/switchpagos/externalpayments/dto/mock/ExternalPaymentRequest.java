package com.banquito.switchpagos.externalpayments.dto.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExternalPaymentRequest(
        UUID uetr,
        String originBankCode,
        String originTransactionId,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency,
        String concept,
        String beneficiaryName,
        LocalDate valueDate) {
}
