package com.banquito.switchpagos.externalpayments.client;

import com.banquito.switchpagos.externalpayments.dto.mock.ExternalPaymentRequest;
import com.banquito.switchpagos.externalpayments.dto.mock.ExternalPaymentResponse;
import com.banquito.switchpagos.externalpayments.enums.ExternalBankStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class MockExternalBankPaymentClient implements ExternalBankPaymentClient {

    private final Map<String, MockPayment> byIdempotency = new ConcurrentHashMap<>();
    private final Map<String, MockPayment> byExternalId = new ConcurrentHashMap<>();

    @Override
    public ExternalPaymentResponse createPayment(String idempotencyKey, ExternalPaymentRequest request) {
        validateContract(request);
        MockPayment existing = byIdempotency.get(idempotencyKey);
        if (existing != null) {
            if (!existing.fingerprint.equals(fingerprint(request))) {
                throw new ExternalBankConflictException("La clave de idempotencia ya existe con un payload diferente.");
            }
            return existing.currentResponse(false);
        }

        MockPayment created = new MockPayment(
                "EXT-" + request.uetr().toString().substring(0, 8).toUpperCase(),
                idempotencyKey,
                fingerprint(request),
                scenarioFor(request.concept()));
        byIdempotency.put(idempotencyKey, created);
        byExternalId.put(created.externalPaymentId, created);
        if (contains(request.concept(), "MOCK_TIMEOUT_THEN_PROCESSED")
                || contains(request.concept(), "MOCK_TIMEOUT_THEN_FAILED")) {
            throw new ExternalBankTimeoutException("Timeout simulado despues de crear la operacion externa.");
        }
        return created.currentResponse(false);
    }

    @Override
    public ExternalPaymentResponse getByExternalPaymentId(String externalPaymentId) {
        MockPayment payment = byExternalId.get(externalPaymentId);
        if (payment == null) {
            throw new ExternalBankClientException("EXTERNAL_PAYMENT_NOT_FOUND", "No existe pago externo mock.");
        }
        return payment.currentResponse(true);
    }

    @Override
    public ExternalPaymentResponse getByIdempotencyKey(String idempotencyKey) {
        MockPayment payment = byIdempotency.get(idempotencyKey);
        if (payment == null) {
            throw new ExternalBankClientException("EXTERNAL_PAYMENT_NOT_FOUND", "No existe pago externo mock para idempotencia.");
        }
        return payment.currentResponse(true);
    }

    private static List<ExternalBankStatus> scenarioFor(String reference) {
        if (contains(reference, "MOCK_FAILED")) {
            return List.of(ExternalBankStatus.FAILED);
        }
        if (contains(reference, "MOCK_REJECTED")) {
            return List.of(ExternalBankStatus.REJECTED);
        }
        if (contains(reference, "MOCK_PROCESSING_THEN_FAILED") || contains(reference, "MOCK_TIMEOUT_THEN_FAILED")) {
            return List.of(ExternalBankStatus.PROCESSING, ExternalBankStatus.PROCESSING, ExternalBankStatus.FAILED);
        }
        if (contains(reference, "MOCK_PROCESSING_THEN_PROCESSED") || contains(reference, "MOCK_TIMEOUT_THEN_PROCESSED")) {
            return List.of(ExternalBankStatus.PROCESSING, ExternalBankStatus.PROCESSING, ExternalBankStatus.PROCESSED);
        }
        return List.of(ExternalBankStatus.PROCESSED);
    }

    private static boolean contains(String value, String token) {
        return value != null && value.toUpperCase().contains(token);
    }

    private static String fingerprint(ExternalPaymentRequest request) {
        return request.uetr() + "|" + request.originBankCode() + "|" + request.originTransactionId() + "|"
                + request.destinationAccountNumber() + "|" + request.amount() + "|" + request.currency() + "|"
                + request.valueDate();
    }

    private static void validateContract(ExternalPaymentRequest request) {
        if (request == null || request.uetr() == null || !isUuidV4(request.uetr())
                || blank(request.originBankCode()) || blank(request.originTransactionId())
                || blank(request.destinationAccountNumber()) || request.amount() == null
                || request.amount().signum() <= 0 || blank(request.currency())
                || blank(request.beneficiaryName()) || request.valueDate() == null) {
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
            String externalPaymentId,
            String idempotencyKey,
            String fingerprint,
            List<ExternalBankStatus> scenario,
            AtomicInteger queryCount) {
        MockPayment(String externalPaymentId, String idempotencyKey, String fingerprint, List<ExternalBankStatus> scenario) {
            this(externalPaymentId, idempotencyKey, fingerprint, scenario, new AtomicInteger(0));
        }

        ExternalPaymentResponse currentResponse(boolean advance) {
            int index = advance ? queryCount.updateAndGet(i -> Math.min(i + 1, scenario.size() - 1)) : queryCount.get();
            ExternalBankStatus status = scenario.get(Math.min(index, scenario.size() - 1));
            String failureCode = switch (status) {
                case FAILED -> "MOCK_BANK_FAILED";
                case REJECTED -> "MOCK_BANK_REJECTED";
                default -> null;
            };
            String message = switch (status) {
                case PROCESSED -> "Pago externo procesado correctamente por la institucion destino.";
                case PROCESSING -> "Pago externo mock en procesamiento.";
                case FAILED -> "El banco externo simulo un fallo definitivo.";
                case REJECTED -> "El banco externo simulo un rechazo funcional.";
            };
            OffsetDateTime processedAt = Objects.equals(status, ExternalBankStatus.PROCESSED) ? OffsetDateTime.now() : null;
            return new ExternalPaymentResponse(externalPaymentId, status.name(), failureCode, message, processedAt);
        }
    }
}
