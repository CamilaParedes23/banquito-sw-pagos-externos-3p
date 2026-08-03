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
    void serializesOnlyRealInterbankContractFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        InterbankPaymentRequest request = request("Pago normal", "22001100");

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"uetr\"");
        assertThat(json).contains("\"originTransactionId\"");
        assertThat(json).contains("\"routingCode\":\"003\"");
        assertThat(json).contains("\"destinationAccountNumber\":\"22001100\"");
        assertThat(json).contains("\"valueDate\"");
        assertThat(json).doesNotContain("sourceRoutingCode");
        assertThat(json).doesNotContain("destinationRoutingCode");
        assertThat(json).doesNotContain("paymentLineUuid");
        assertThat(json).doesNotContain("sourceAccountNumber");
        assertThat(json).doesNotContain("beneficiaryIdentification");
    }

    @Test
    void rejectsInvalidInterbankContract() {
        InterbankPaymentRequest request = new InterbankPaymentRequest(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                "003",
                "22001100",
                new BigDecimal("10.50"),
                "USD",
                "Pago normal",
                "Beneficiario Uno",
                LocalDate.now(),
                UUID.fromString("00000000-0000-3000-8000-000000000001"),
                UUID.randomUUID(),
                "1010100001",
                "1790012345001",
                "Empresa Uno",
                "0102030405",
                "beneficiario@example.com",
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
                paymentLineId.toString(),
                "003",
                destinationAccount,
                new BigDecimal("10.50"),
                "USD",
                reference,
                "Beneficiario Uno",
                LocalDate.now(),
                paymentLineId,
                UUID.randomUUID(),
                "1010100001",
                "1790012345001",
                "Empresa Uno",
                "0102030405",
                "beneficiario@example.com",
                UUID.randomUUID());
    }
}
