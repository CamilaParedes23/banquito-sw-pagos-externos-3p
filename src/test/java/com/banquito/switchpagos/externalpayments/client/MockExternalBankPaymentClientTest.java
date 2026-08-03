package com.banquito.switchpagos.externalpayments.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.banquito.switchpagos.externalpayments.dto.interbank.InterbankPaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MockExternalBankPaymentClientTest {

    private final MockExternalBankPaymentClient client = new MockExternalBankPaymentClient();

    @Test
    void createsSettledPaymentByDefault() {
        InterbankPaymentRequest request = request("Pago normal", "22001100");

        var response = client.createPayment(request.paymentLineUuid().toString(), request);

        assertThat(response.status()).isEqualTo("SETTLED");
        assertThat(response.interbankTransferUuid()).isNotNull();
        assertThat(response.processedAt()).isNotNull();
    }

    @Test
    void createsRejectedPaymentFromFailureReferenceRule() {
        InterbankPaymentRequest request = request("MOCK_FAILED", "22001100");

        var response = client.createPayment(request.paymentLineUuid().toString(), request);

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(response.errorCode()).isEqualTo("MOCK_BANK_REJECTED");
    }

    @Test
    void advancesPreparedScenarioUntilSettled() {
        InterbankPaymentRequest request = request("MOCK_PROCESSING_THEN_PROCESSED", "22001100");
        UUID paymentLineId = request.paymentLineUuid();

        var initial = client.createPayment(paymentLineId.toString(), request);
        var firstPoll = client.getByPaymentLineId(paymentLineId);
        var secondPoll = client.getByPaymentLineId(paymentLineId);

        assertThat(initial.status()).isEqualTo("PREPARED");
        assertThat(firstPoll.status()).isEqualTo("PREPARED");
        assertThat(secondPoll.status()).isEqualTo("SETTLED");
    }

    @Test
    void advancesPreparedScenarioUntilRejected() {
        InterbankPaymentRequest request = request("MOCK_PROCESSING_THEN_FAILED", "22001100");
        UUID paymentLineId = request.paymentLineUuid();

        client.createPayment(paymentLineId.toString(), request);
        client.getByPaymentLineId(paymentLineId);
        var finalPoll = client.getByPaymentLineId(paymentLineId);

        assertThat(finalPoll.status()).isEqualTo("REJECTED");
        assertThat(finalPoll.errorCode()).isEqualTo("MOCK_BANK_REJECTED");
    }

    @Test
    void timeoutAfterCreateCanRecoverByPaymentLineId() {
        InterbankPaymentRequest request = request("MOCK_TIMEOUT_THEN_PROCESSED", "22001100");
        UUID paymentLineId = request.paymentLineUuid();

        assertThatThrownBy(() -> client.createPayment(paymentLineId.toString(), request))
                .isInstanceOf(ExternalBankTimeoutException.class);

        assertThat(client.getByPaymentLineId(paymentLineId).status()).isEqualTo("PREPARED");
        assertThat(client.getByPaymentLineId(paymentLineId).status()).isEqualTo("SETTLED");
    }

    @Test
    void sameIdempotencyKeyWithDifferentFingerprintIsConflict() {
        InterbankPaymentRequest request = request("Pago normal", "22001100");
        String key = request.paymentLineUuid().toString();
        client.createPayment(key, request);

        assertThatThrownBy(() -> client.createPayment(key, requestWithLineId(request.paymentLineUuid(), "Pago normal", "99990000")))
                .isInstanceOf(ExternalBankConflictException.class);
    }

    @Test
    void sameIdempotencyKeyWithSameFingerprintIsReplay() {
        InterbankPaymentRequest request = request("Pago normal", "22001100");
        String key = request.paymentLineUuid().toString();
        client.createPayment(key, request);

        var replay = client.createPayment(key, request);

        assertThat(replay.idempotencyReplayed()).isTrue();
    }

    @Test
    void serializesOnlyRealInterbankContractFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        InterbankPaymentRequest request = request("Pago normal", "22001100");

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"sourceTransferUuid\"");
        assertThat(json).contains("\"paymentLineUuid\"");
        assertThat(json).contains("\"sourceRoutingCode\":\"003\"");
        assertThat(json).contains("\"destinationRoutingCode\":\"001\"");
        assertThat(json).contains("\"sourceAccountNumber\":\"1010100001\"");
        assertThat(json).contains("\"destinationAccountNumber\":\"22001100\"");
        assertThat(json).contains("\"beneficiaryIdentification\":\"0102030405\"");
        assertThat(json).contains("\"accountingDate\"");
        assertThat(json).doesNotContain("\"uetr\"");
        assertThat(json).doesNotContain("\"originTransactionId\"");
        assertThat(json).doesNotContain("\"routingCode\"");
        assertThat(json).doesNotContain("\"valueDate\"");
    }

    @Test
    void rejectsInvalidInterbankContract() {
        InterbankPaymentRequest request = new InterbankPaymentRequest(
                UUID.randomUUID(),
                UUID.fromString("00000000-0000-3000-8000-000000000001"),
                UUID.randomUUID(),
                "003",
                "001",
                "1010100001",
                "22001100",
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

        assertThatThrownBy(() -> client.createPayment(request.paymentLineUuid().toString(), request))
                .isInstanceOf(ExternalBankClientException.class)
                .hasMessageContaining("contrato requerido");
    }

    private static InterbankPaymentRequest request(String reference, String destinationAccount) {
        return requestWithLineId(UUID.randomUUID(), reference, destinationAccount);
    }

    private static InterbankPaymentRequest requestWithLineId(UUID paymentLineId, String reference, String destinationAccount) {
        return new InterbankPaymentRequest(
                UUID.randomUUID(),
                paymentLineId,
                UUID.randomUUID(),
                "003",
                "001",
                "1010100001",
                destinationAccount,
                "1790012345001",
                "Empresa Uno",
                "0102030405",
                "Beneficiario Uno",
                "beneficiario@example.com",
                reference,
                new BigDecimal("10.50"),
                "USD",
                LocalDate.now(),
                UUID.randomUUID());
    }
}
