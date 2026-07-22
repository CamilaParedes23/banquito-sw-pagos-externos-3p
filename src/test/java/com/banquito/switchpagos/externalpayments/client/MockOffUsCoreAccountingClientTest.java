package com.banquito.switchpagos.externalpayments.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.banquito.switchpagos.externalpayments.dto.mock.CoreAccountingRequest;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MockOffUsCoreAccountingClientTest {

    @Test
    void registersCoreAccountingSuccessfullyByDefault() {
        MockOffUsCoreAccountingClient client = new MockOffUsCoreAccountingClient(true);
        CoreAccountingRequest request = request("Pago normal", "OFFUS-CORE-1");

        var response = client.registerProcessedOffUs(request);

        assertThat(response.success()).isTrue();
        assertThat(response.code()).isEqualTo("00");
        assertThat(response.coreTransactionId()).startsWith("MOCK-CORE-OFFUS-");
    }

    @Test
    void simulatesFunctionalCoreFailureFromReference() {
        MockOffUsCoreAccountingClient client = new MockOffUsCoreAccountingClient(true);

        var response = client.registerProcessedOffUs(request("MOCK_CORE_FAIL", "OFFUS-CORE-2"));

        assertThat(response.success()).isFalse();
        assertThat(response.code()).isEqualTo("CORE_ACCOUNTING_FAILED");
    }

    @Test
    void duplicateCoreRegistrationIsIdempotent() {
        MockOffUsCoreAccountingClient client = new MockOffUsCoreAccountingClient(true);
        CoreAccountingRequest request = request("Pago normal", "OFFUS-CORE-3");

        var first = client.registerProcessedOffUs(request);
        var second = client.registerProcessedOffUs(request);

        assertThat(second).isEqualTo(first);
    }

    private static CoreAccountingRequest request(String reference, String idempotencyKey) {
        return new CoreAccountingRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                "EXT-1",
                "1100220033",
                "22001100",
                "30",
                new BigDecimal("10.50"),
                "USD",
                OffsetDateTime.now(),
                idempotencyKey,
                reference);
    }
}
