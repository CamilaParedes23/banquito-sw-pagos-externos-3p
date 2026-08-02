package com.banquito.switchpagos.externalpayments.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.banquito.switchpagos.externalpayments.dto.mock.ExternalPaymentRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PayloadSanitizerTest {

    private final PayloadSanitizer sanitizer = new PayloadSanitizer();

    @Test
    void masksAccountsAndBeneficiaryIdentification() {
        ExternalPaymentRequest request = new ExternalPaymentRequest(
                UUID.randomUUID(),
                "BQTO",
                "TX-123",
                "2200110099",
                new BigDecimal("10.50"),
                "USD",
                "Pago normal",
                "Beneficiario Uno",
                LocalDate.now());

        String sanitized = sanitizer.sanitize(request);

        assertThat(sanitized).contains("destinationAccountNumber=****0099");
        assertThat(sanitized).doesNotContain("2200110099");
    }

    @Test
    void limitsStoredPayloadLength() {
        String sanitized = sanitizer.sanitize("x".repeat(5000));

        assertThat(sanitized).hasSize(4000);
    }
}
