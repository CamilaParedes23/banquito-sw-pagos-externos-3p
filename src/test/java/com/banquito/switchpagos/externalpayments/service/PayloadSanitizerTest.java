package com.banquito.switchpagos.externalpayments.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.banquito.switchpagos.externalpayments.dto.interbank.InterbankPaymentRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PayloadSanitizerTest {

    private final PayloadSanitizer sanitizer = new PayloadSanitizer();

    @Test
    void masksAccountsAndBeneficiaryIdentification() {
        InterbankPaymentRequest request = new InterbankPaymentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "003",
                "001",
                "1010100001",
                "2200110099",
                "1790012345001",
                "Empresa Uno",
                "0102030405",
                "Beneficiario Uno",
                "beneficiario@example.com",
                "Pago normal",
                new BigDecimal("10.50"),
                "USD",
                LocalDate.now(),
                UUID.randomUUID());

        String sanitized = sanitizer.sanitize(request);

        assertThat(sanitized).contains("destinationAccountNumber=****0099");
        assertThat(sanitized).contains("sourceAccountNumber=****0001");
        assertThat(sanitized).doesNotContain("2200110099");
    }

    @Test
    void limitsStoredPayloadLength() {
        String sanitized = sanitizer.sanitize("x".repeat(5000));

        assertThat(sanitized).hasSize(4000);
    }
}
