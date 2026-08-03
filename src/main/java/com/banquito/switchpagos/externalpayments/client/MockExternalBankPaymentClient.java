package com.banquito.switchpagos.externalpayments.client;

import com.banquito.switchpagos.externalpayments.dto.interbank.InterbankPaymentRequest;
import com.banquito.switchpagos.externalpayments.dto.interbank.InterbankPaymentResponse;
import com.banquito.switchpagos.externalpayments.enums.ExternalBankStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "external.bank.client.mode", havingValue = "mock", matchIfMissing = true)
public class MockExternalBankPaymentClient implements ExternalBankPaymentClient {

    private final Map<String, MockPayment> byIdempotency = new ConcurrentHashMap<>();
    private final Map<UUID, MockPayment> byPaymentLineId = new ConcurrentHashMap<>();

    @Override
    public InterbankPaymentResponse createPayment(String idempotencyKey, InterbankPaymentRequest request) {
        validateContract(request);
        MockPayment existing = byIdempotency.get(idempotencyKey);
        if (existing != null) {
            if (!existing.fingerprint.equals(fingerprint(request))) {
                throw new ExternalBankConflictException("La clave de idempotencia ya existe con un payload diferente.");
            }
            return existing.currentResponse(false);
        }

        MockPayment created = new MockPayment(
                UUID.nameUUIDFromBytes(("MOCK-" + request.paymentLineUuid()).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                idempotencyKey,
                fingerprint(request),
                scenarioFor(request.concept()));
        byIdempotency.put(idempotencyKey, created);
        byPaymentLineId.put(request.paymentLineUuid(), created);
        if (contains(request.concept(), "MOCK_TIMEOUT_THEN_PROCESSED")
                || contains(request.concept(), "MOCK_TIMEOUT_THEN_FAILED")) {
            throw new ExternalBankTimeoutException("Timeout simulado despues de crear la operacion externa.");
        }
        return created.currentResponse(false);
    }

    @Override
    public InterbankPaymentResponse getByPaymentLineId(UUID paymentLineId) {
        MockPayment payment = byPaymentLineId.get(paymentLineId);
        if (payment == null) {
            throw new ExternalBankClientException("EXTERNAL_PAYMENT_NOT_FOUND", "No existe pago externo mock.");
        }
        return payment.currentResponse(true);
    }

    private static List<ExternalBankStatus> scenarioFor(String reference) {
        if (contains(reference, "MOCK_FAILED")) {
            return List.of(ExternalBankStatus.REJECTED);
        }
        if (contains(reference, "MOCK_REJECTED")) {
            return List.of(ExternalBankStatus.REJECTED);
        }
        if (contains(reference, "MOCK_PROCESSING_THEN_FAILED") || contains(reference, "MOCK_TIMEOUT_THEN_FAILED")) {
            return List.of(ExternalBankStatus.PREPARED, ExternalBankStatus.PREPARED, ExternalBankStatus.REJECTED);
        }
        if (contains(reference, "MOCK_PROCESSING_THEN_PROCESSED") || contains(reference, "MOCK_TIMEOUT_THEN_PROCESSED")) {
            return List.of(ExternalBankStatus.PREPARED, ExternalBankStatus.PREPARED, ExternalBankStatus.SETTLED);
        }
        return List.of(ExternalBankStatus.SETTLED);
    }

    private static boolean contains(String value, String token) {
        return value != null && value.toUpperCase().contains(token);
    }

    private static String fingerprint(InterbankPaymentRequest request) {
        return request.sourceTransferUuid() + "|" + request.paymentLineUuid() + "|" + request.batchUuid() + "|"
                + request.sourceRoutingCode() + "|" + request.destinationRoutingCode() + "|"
                + request.destinationAccountNumber() + "|" + request.amount() + "|" + request.currency() + "|"
                + request.accountingDate();
    }

    private static void validateContract(InterbankPaymentRequest request) {
        if (request == null || request.sourceTransferUuid() == null || request.paymentLineUuid() == null
                || !isUuidV4(request.paymentLineUuid()) || blank(request.sourceRoutingCode())
                || blank(request.destinationRoutingCode()) || blank(request.destinationAccountNumber()) || request.amount() == null
                || request.amount().signum() <= 0 || blank(request.currency())
                || blank(request.beneficiaryIdentification()) || blank(request.beneficiaryName())
                || request.accountingDate() == null || request.correlationId() == null) {
            throw new ExternalBankClientException(
                    "INVALID_INTERBANK_PAYMENT_REQUEST",
                    "La solicitud interbancaria no cumple el contrato requerido.");
        }
    }

    private static boolean isUuidV4(UUID uuid) {
        return uuid.version() == 4;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record MockPayment(
            UUID externalPaymentId,
            String idempotencyKey,
            String fingerprint,
            List<ExternalBankStatus> scenario,
            AtomicInteger queryCount) {
        MockPayment(UUID externalPaymentId, String idempotencyKey, String fingerprint, List<ExternalBankStatus> scenario) {
            this(externalPaymentId, idempotencyKey, fingerprint, scenario, new AtomicInteger(0));
        }

        InterbankPaymentResponse currentResponse(boolean advance) {
            int index = advance ? queryCount.updateAndGet(i -> Math.min(i + 1, scenario.size() - 1)) : queryCount.get();
            ExternalBankStatus status = scenario.get(Math.min(index, scenario.size() - 1));
            String failureCode = switch (status) {
                case REJECTED -> "MOCK_BANK_REJECTED";
                default -> null;
            };
            String message = switch (status) {
                case SETTLED -> "Pago externo procesado correctamente por la institucion destino.";
                case PREPARED -> "Pago externo mock en procesamiento.";
                case REJECTED -> "El banco externo simulo un rechazo funcional.";
            };
            OffsetDateTime processedAt = Objects.equals(status, ExternalBankStatus.SETTLED) ? OffsetDateTime.now() : null;
            return new InterbankPaymentResponse(
                    externalPaymentId,
                    null,
                    null,
                    null,
                    "SALIENTE",
                    status.name(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    status == ExternalBankStatus.SETTLED ? externalPaymentId : null,
                    null,
                    null,
                    null,
                    status == ExternalBankStatus.SETTLED ? "IBI-" + externalPaymentId.toString().substring(0, 8).toUpperCase() : null,
                    failureCode,
                    message,
                    null,
                    processedAt,
                    false,
                    null);
        }
    }
}
