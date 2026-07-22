package com.banquito.switchpagos.externalpayments.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.banquito.switchpagos.externalpayments.dto.mock.ExternalPaymentRequest;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MockExternalBankPaymentClientTest {

    private final MockExternalBankPaymentClient client = new MockExternalBankPaymentClient();

    @Test
    void createsProcessedPaymentByDefault() {
        ExternalPaymentRequest request = request("Pago normal", "22001100");

        var response = client.createPayment("OFFUS-" + request.lineId(), request);

        assertThat(response.status()).isEqualTo("PROCESSED");
        assertThat(response.externalPaymentId()).startsWith("EXT-");
        assertThat(response.processedAt()).isNotNull();
    }

    @Test
    void createsFailedPaymentFromReferenceRule() {
        ExternalPaymentRequest request = request("MOCK_FAILED", "22001100");

        var response = client.createPayment("OFFUS-" + request.lineId(), request);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.failureCode()).isEqualTo("MOCK_BANK_FAILED");
    }

    @Test
    void createsRejectedPaymentFromReferenceRule() {
        ExternalPaymentRequest request = request("MOCK_REJECTED", "22001100");

        var response = client.createPayment("OFFUS-" + request.lineId(), request);

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(response.failureCode()).isEqualTo("MOCK_BANK_REJECTED");
    }

    @Test
    void advancesProcessingScenarioUntilProcessed() {
        ExternalPaymentRequest request = request("MOCK_PROCESSING_THEN_PROCESSED", "22001100");
        String key = "OFFUS-" + request.lineId();

        var initial = client.createPayment(key, request);
        var firstPoll = client.getByIdempotencyKey(key);
        var secondPoll = client.getByExternalPaymentId(initial.externalPaymentId());

        assertThat(initial.status()).isEqualTo("PROCESSING");
        assertThat(firstPoll.status()).isEqualTo("PROCESSING");
        assertThat(secondPoll.status()).isEqualTo("PROCESSED");
    }

    @Test
    void advancesProcessingScenarioUntilFailed() {
        ExternalPaymentRequest request = request("MOCK_PROCESSING_THEN_FAILED", "22001100");
        String key = "OFFUS-" + request.lineId();

        client.createPayment(key, request);
        client.getByIdempotencyKey(key);
        var finalPoll = client.getByIdempotencyKey(key);

        assertThat(finalPoll.status()).isEqualTo("FAILED");
        assertThat(finalPoll.failureCode()).isEqualTo("MOCK_BANK_FAILED");
    }

    @Test
    void timeoutAfterCreateCanRecoverByIdempotency() {
        ExternalPaymentRequest request = request("MOCK_TIMEOUT_THEN_PROCESSED", "22001100");
        String key = "OFFUS-" + request.lineId();

        assertThatThrownBy(() -> client.createPayment(key, request))
                .isInstanceOf(ExternalBankTimeoutException.class);

        assertThat(client.getByIdempotencyKey(key).status()).isEqualTo("PROCESSING");
        assertThat(client.getByIdempotencyKey(key).status()).isEqualTo("PROCESSED");
    }

    @Test
    void sameIdempotencyKeyWithDifferentFingerprintIsConflict() {
        ExternalPaymentRequest request = request("Pago normal", "22001100");
        String key = "OFFUS-" + request.lineId();
        client.createPayment(key, request);

        assertThatThrownBy(() -> client.createPayment(key, request("Pago normal", "99990000")))
                .isInstanceOf(ExternalBankConflictException.class);
    }

    private static ExternalPaymentRequest request(String reference, String destinationAccount) {
        return new ExternalPaymentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BQTO",
                "PICH",
                "1100220033",
                destinationAccount,
                "1717171717",
                "Beneficiario Uno",
                new BigDecimal("10.50"),
                "USD",
                reference);
    }
}
