package com.banquito.switchpagos.externalpayments.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.banquito.switchpagos.externalpayments.dto.mock.ExternalPaymentRequest;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PayloadSanitizerTest {

    private final PayloadSanitizer sanitizer = new PayloadSanitizer();

    @Test
    void masksAccountsAndBeneficiaryIdentification() {
        ExternalPaymentRequest request = new ExternalPaymentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BQTO",
                "PICH",
                "1100220033",
                "2200110099",
                "1717171717",
                "Beneficiario Uno",
                new BigDecimal("10.50"),
                "USD",
                "Pago normal");

        String sanitized = sanitizer.sanitize(request);

        assertThat(sanitized).contains("sourceAccountNumber=****0033");
        assertThat(sanitized).contains("destinationAccountNumber=****0099");
        assertThat(sanitized).contains("beneficiaryIdentification=****1717");
        assertThat(sanitized).doesNotContain("1100220033");
        assertThat(sanitized).doesNotContain("2200110099");
        assertThat(sanitized).doesNotContain("1717171717");
    }

    @Test
    void limitsStoredPayloadLength() {
        String sanitized = sanitizer.sanitize("x".repeat(5000));

        assertThat(sanitized).hasSize(4000);
    }
}
