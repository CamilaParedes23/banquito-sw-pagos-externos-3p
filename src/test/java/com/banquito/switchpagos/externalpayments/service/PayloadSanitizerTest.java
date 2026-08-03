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
                UUID.randomUUID().toString(),
                "003",
                "2200110099",
                new BigDecimal("10.50"),
                "USD",
                "Pago normal",
                "Beneficiario Uno",
                LocalDate.now(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "1010100001",
                "1790012345001",
                "Empresa Uno",
                "0102030405",
                "beneficiario@example.com",
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
