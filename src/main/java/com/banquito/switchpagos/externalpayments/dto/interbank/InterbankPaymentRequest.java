package com.banquito.switchpagos.externalpayments.dto.interbank;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InterbankPaymentRequest(
        @JsonProperty("uetr") UUID sourceTransferUuid,
        String originTransactionId,
        String routingCode,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency,
        String concept,
        String beneficiaryName,
        @JsonProperty("valueDate") LocalDate accountingDate,
        @JsonIgnore
        UUID paymentLineUuid,
        @JsonIgnore
        UUID batchUuid,
        @JsonIgnore
        String sourceAccountNumber,
        @JsonIgnore
        String originatorIdentification,
        @JsonIgnore
        String originatorName,
        @JsonIgnore
        String beneficiaryIdentification,
        @JsonIgnore
        String beneficiaryEmail,
        @JsonIgnore
        UUID correlationId) {
}
