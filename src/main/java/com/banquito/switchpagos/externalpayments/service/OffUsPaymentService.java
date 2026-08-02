package com.banquito.switchpagos.externalpayments.service;

import com.banquito.switchpagos.externalpayments.client.ExternalBankClientException;
import com.banquito.switchpagos.externalpayments.client.ExternalBankPaymentClient;
import com.banquito.switchpagos.externalpayments.client.ExternalBankTimeoutException;
import com.banquito.switchpagos.externalpayments.client.OffUsCoreAccountingClient;
import com.banquito.switchpagos.externalpayments.dto.event.OffUsPaymentCompletedEvent;
import com.banquito.switchpagos.externalpayments.dto.event.OffUsPaymentFailedEvent;
import com.banquito.switchpagos.externalpayments.dto.event.PaymentLineRoutedOffUsEvent;
import com.banquito.switchpagos.externalpayments.dto.mock.CoreAccountingRequest;
import com.banquito.switchpagos.externalpayments.dto.mock.CoreAccountingResponse;
import com.banquito.switchpagos.externalpayments.dto.mock.ExternalPaymentRequest;
import com.banquito.switchpagos.externalpayments.dto.mock.ExternalPaymentResponse;
import com.banquito.switchpagos.externalpayments.enums.AttemptOperationType;
import com.banquito.switchpagos.externalpayments.enums.CoreAccountingStatus;
import com.banquito.switchpagos.externalpayments.enums.ExternalBankStatus;
import com.banquito.switchpagos.externalpayments.enums.FailureStage;
import com.banquito.switchpagos.externalpayments.enums.PaymentStatus;
import com.banquito.switchpagos.externalpayments.model.OffUsPayment;
import com.banquito.switchpagos.externalpayments.model.OffUsPaymentAttempt;
import com.banquito.switchpagos.externalpayments.repository.OffUsPaymentAttemptRepository;
import com.banquito.switchpagos.externalpayments.repository.OffUsPaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OffUsPaymentService {
    private static final Set<String> FINAL_STATUSES = Set.of(PaymentStatus.PROCESADA.name(), PaymentStatus.FALLIDA_BANCO.name(), PaymentStatus.FALLIDA_CORE.name());

    private final OffUsPaymentRepository paymentRepository;
    private final OffUsPaymentAttemptRepository attemptRepository;
    private final ExternalBankPaymentClient externalBankClient;
    private final OffUsCoreAccountingClient coreAccountingClient;
    private final OffUsPaymentResultPublisher resultPublisher;
    private final PayloadSanitizer sanitizer;
    private final String originBankCode;
    private final Integer maxStatusQueryAttempts;
    private final Long pollIntervalMs;

    public OffUsPaymentService(
            OffUsPaymentRepository paymentRepository,
            OffUsPaymentAttemptRepository attemptRepository,
            ExternalBankPaymentClient externalBankClient,
            OffUsCoreAccountingClient coreAccountingClient,
            OffUsPaymentResultPublisher resultPublisher,
            PayloadSanitizer sanitizer,
            @Value("${external.payment.mock.origin-bank-code}") String originBankCode,
            @Value("${external.payment.poll.max-attempts}") Integer maxStatusQueryAttempts,
            @Value("${external.payment.poll.interval-ms}") Long pollIntervalMs) {
        this.paymentRepository = paymentRepository;
        this.attemptRepository = attemptRepository;
        this.externalBankClient = externalBankClient;
        this.coreAccountingClient = coreAccountingClient;
        this.resultPublisher = resultPublisher;
        this.sanitizer = sanitizer;
        this.originBankCode = originBankCode;
        this.maxStatusQueryAttempts = maxStatusQueryAttempts;
        this.pollIntervalMs = pollIntervalMs;
    }

    @Transactional
    public void processRoutedOffUsLine(PaymentLineRoutedOffUsEvent event) {
        validate(event);
        OffUsPayment payment = paymentRepository.findByLineId(event.lineId).orElse(null);
        if (payment == null) {
            payment = createPayment(event);
            paymentRepository.save(payment);
        } else if (Boolean.TRUE.equals(payment.getFinalResultPublished())) {
            return;
        }
        if (FINAL_STATUSES.contains(payment.getStatus())) {
            publishFinalIfNeeded(payment);
            return;
        }
        createExternalPayment(payment);
    }

    @Transactional
    public OffUsPayment resume(UUID id) {
        OffUsPayment payment = paymentRepository.findById(id).orElseThrow();
        if (Boolean.TRUE.equals(payment.getFinalResultPublished())) {
            return payment;
        }
        if (PaymentStatus.PENDIENTE_BANCO.name().equals(payment.getStatus())
                || PaymentStatus.CONSULTANDO_BANCO.name().equals(payment.getStatus())
                || PaymentStatus.ERROR_TECNICO.name().equals(payment.getStatus())) {
            queryExternalStatus(payment);
        }
        return paymentRepository.save(payment);
    }

    @Transactional
    public void pollPendingPayments() {
        List<String> statuses = List.of(PaymentStatus.PENDIENTE_BANCO.name(), PaymentStatus.CONSULTANDO_BANCO.name(), PaymentStatus.ERROR_TECNICO.name());
        for (OffUsPayment payment : paymentRepository.findByStatusInAndNextStatusQueryAtLessThanEqual(statuses, OffsetDateTime.now())) {
            if (!Boolean.TRUE.equals(payment.getFinalResultPublished())) {
                queryExternalStatus(payment);
            }
        }
    }

    private void createExternalPayment(OffUsPayment payment) {
        ExternalPaymentRequest request = toExternalRequest(payment);
        OffUsPaymentAttempt attempt = startAttempt(payment, AttemptOperationType.CREATE_PAYMENT, request);
        payment.setStatus(PaymentStatus.ENVIANDO_BANCO.name());
        payment.setUpdatedAt(OffsetDateTime.now());
        try {
            ExternalPaymentResponse response = externalBankClient.createPayment(payment.getIdempotencyKey(), request);
            finishAttempt(attempt, true, 200, response.status(), null, null, response);
            handleExternalResponse(payment, response, FailureStage.EXTERNAL_BANK);
        } catch (ExternalBankTimeoutException exception) {
            finishAttempt(attempt, false, 504, null, exception.getCode(), exception.getMessage(), null);
            payment.setStatus(PaymentStatus.PENDIENTE_BANCO.name());
            scheduleNextQuery(payment);
            queryExternalStatus(payment);
        } catch (ExternalBankClientException exception) {
            finishAttempt(attempt, false, 409, null, exception.getCode(), exception.getMessage(), null);
            fail(payment, FailureStage.EXTERNAL_BANK, exception.getCode(), exception.getMessage());
        }
        paymentRepository.save(payment);
    }

    private void queryExternalStatus(OffUsPayment payment) {
        if (payment.getStatusQueryAttempts() >= maxStatusQueryAttempts) {
            payment.setStatus(PaymentStatus.ERROR_TECNICO.name());
            payment.setExternalFailureCode("STATUS_QUERY_EXHAUSTED");
            payment.setExternalMessage("Consultas de estado agotadas sin resultado definitivo.");
            payment.setUpdatedAt(OffsetDateTime.now());
            return;
        }
        payment.setStatus(PaymentStatus.CONSULTANDO_BANCO.name());
        payment.setStatusQueryAttempts(payment.getStatusQueryAttempts() + 1);
        AttemptOperationType type = payment.getExternalPaymentId() == null
                ? AttemptOperationType.QUERY_BY_IDEMPOTENCY
                : AttemptOperationType.QUERY_BY_EXTERNAL_ID;
        Object request = payment.getExternalPaymentId() == null ? payment.getIdempotencyKey() : payment.getExternalPaymentId();
        OffUsPaymentAttempt attempt = startAttempt(payment, type, request);
        try {
            ExternalPaymentResponse response = payment.getExternalPaymentId() == null
                    ? externalBankClient.getByIdempotencyKey(payment.getIdempotencyKey())
                    : externalBankClient.getByExternalPaymentId(payment.getExternalPaymentId());
            finishAttempt(attempt, true, 200, response.status(), null, null, response);
            handleExternalResponse(payment, response, FailureStage.STATUS_QUERY);
        } catch (ExternalBankClientException exception) {
            finishAttempt(attempt, false, 404, null, exception.getCode(), exception.getMessage(), null);
            payment.setStatus(PaymentStatus.ERROR_TECNICO.name());
            payment.setExternalFailureCode(exception.getCode());
            payment.setExternalMessage(limit(exception.getMessage(), 500));
            scheduleNextQuery(payment);
        }
        paymentRepository.save(payment);
    }

    private void handleExternalResponse(OffUsPayment payment, ExternalPaymentResponse response, FailureStage failureStage) {
        payment.setExternalPaymentId(response.externalPaymentId());
        payment.setExternalStatus(response.status());
        payment.setExternalFailureCode(response.failureCode());
        payment.setExternalMessage(limit(response.message(), 500));
        payment.setExternalProcessedAt(response.processedAt());
        ExternalBankStatus status = ExternalBankStatus.valueOf(response.status());
        if (status == ExternalBankStatus.PROCESSING) {
            payment.setStatus(PaymentStatus.PENDIENTE_BANCO.name());
            scheduleNextQuery(payment);
            return;
        }
        if (status == ExternalBankStatus.FAILED || status == ExternalBankStatus.REJECTED) {
            payment.setStatus(status == ExternalBankStatus.FAILED ? PaymentStatus.FALLIDA_BANCO.name() : PaymentStatus.RECHAZADA_BANCO.name());
            fail(payment, failureStage, response.failureCode(), response.message());
            return;
        }
        payment.setStatus(PaymentStatus.PROCESADA_BANCO.name());
        registerCoreAccounting(payment);
    }

    private void registerCoreAccounting(OffUsPayment payment) {
        payment.setStatus(PaymentStatus.REGISTRANDO_CORE.name());
        CoreAccountingRequest request = new CoreAccountingRequest(
                payment.getBatchId(),
                payment.getLineId(),
                payment.getCoreFundingId(),
                payment.getExternalPaymentId(),
                payment.getSourceAccountNumber(),
                payment.getDestinationAccountNumber(),
                payment.getRoutingCode(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getExternalProcessedAt(),
                "OFFUS-CORE-" + payment.getLineId(),
                payment.getReference());
        OffUsPaymentAttempt attempt = startAttempt(payment, AttemptOperationType.REGISTER_CORE_ACCOUNTING, request);
        CoreAccountingResponse response = coreAccountingClient.registerProcessedOffUs(request);
        finishAttempt(attempt, response.success(), response.success() ? 200 : 409, null, response.code(), response.message(), response);
        if (response.success()) {
            payment.setCoreAccountingStatus(CoreAccountingStatus.REGISTRADA.name());
            payment.setCoreTransactionId(response.coreTransactionId());
            payment.setStatus(PaymentStatus.PROCESADA.name());
            payment.setCompletedAt(OffsetDateTime.now());
            publishFinalIfNeeded(payment);
        } else {
            payment.setCoreAccountingStatus(CoreAccountingStatus.FALLIDA.name());
            payment.setStatus(PaymentStatus.FALLIDA_CORE.name());
            fail(payment, FailureStage.CORE_ACCOUNTING, "CORE_ACCOUNTING_FAILED", response.message());
        }
    }

    private void fail(OffUsPayment payment, FailureStage stage, String code, String message) {
        if (!PaymentStatus.FALLIDA_CORE.name().equals(payment.getStatus())
                && !PaymentStatus.FALLIDA_BANCO.name().equals(payment.getStatus())) {
            payment.setStatus(PaymentStatus.FALLIDA_BANCO.name());
        }
        payment.setExternalFailureCode(limit(code, 80));
        payment.setExternalMessage(limit(message, 500));
        payment.setCompletedAt(OffsetDateTime.now());
        publishFailedIfNeeded(payment, stage, code, message);
    }

    private void publishFinalIfNeeded(OffUsPayment payment) {
        if (Boolean.TRUE.equals(payment.getFinalResultPublished())) {
            return;
        }
        OffUsPaymentCompletedEvent event = new OffUsPaymentCompletedEvent();
        event.eventId = UUID.randomUUID();
        event.eventType = "OFF_US_PAYMENT_COMPLETED";
        event.occurredAt = OffsetDateTime.now();
        copyCommon(payment, event);
        event.externalPaymentId = payment.getExternalPaymentId();
        event.externalProcessedAt = payment.getExternalProcessedAt();
        event.coreTransactionId = payment.getCoreTransactionId();
        event.result = "PROCESADA_OFF_US";
        event.billable = true;
        resultPublisher.publishCompleted(event);
        payment.setFinalResultPublished(true);
        payment.setUpdatedAt(OffsetDateTime.now());
    }

    private void publishFailedIfNeeded(OffUsPayment payment, FailureStage stage, String code, String message) {
        if (Boolean.TRUE.equals(payment.getFinalResultPublished())) {
            return;
        }
        OffUsPaymentFailedEvent event = new OffUsPaymentFailedEvent();
        event.eventId = UUID.randomUUID();
        event.eventType = "OFF_US_PAYMENT_FAILED";
        event.occurredAt = OffsetDateTime.now();
        copyCommon(payment, event);
        event.externalPaymentId = payment.getExternalPaymentId();
        event.failureStage = stage.name();
        event.failureCode = limit(code, 80);
        event.failureMessage = userSafeMessage(stage, message);
        event.result = "FALLIDA";
        event.billable = false;
        resultPublisher.publishFailed(event);
        payment.setFinalResultPublished(true);
        payment.setUpdatedAt(OffsetDateTime.now());
    }

    private void copyCommon(OffUsPayment payment, OffUsPaymentCompletedEvent event) {
        event.batchId = payment.getBatchId();
        event.lineId = payment.getLineId();
        event.correlationId = payment.getCorrelationId();
        event.sequenceNumber = payment.getSequenceNumber();
        event.companyRuc = payment.getCompanyRuc();
        event.coreFundingId = payment.getCoreFundingId();
        event.routingCode = payment.getRoutingCode();
        event.destinationInstitutionName = payment.getDestinationInstitutionName();
        event.destinationAccountNumber = payment.getDestinationAccountNumber();
        event.beneficiaryIdentification = payment.getBeneficiaryIdentification();
        event.beneficiaryName = payment.getBeneficiaryName();
        event.amount = payment.getAmount();
        event.currency = payment.getCurrency();
        event.reference = payment.getReference();
        event.notificationEmail = payment.getNotificationEmail();
    }

    private void copyCommon(OffUsPayment payment, OffUsPaymentFailedEvent event) {
        event.batchId = payment.getBatchId();
        event.lineId = payment.getLineId();
        event.correlationId = payment.getCorrelationId();
        event.sequenceNumber = payment.getSequenceNumber();
        event.companyRuc = payment.getCompanyRuc();
        event.coreFundingId = payment.getCoreFundingId();
        event.routingCode = payment.getRoutingCode();
        event.destinationInstitutionName = payment.getDestinationInstitutionName();
        event.destinationAccountNumber = payment.getDestinationAccountNumber();
        event.beneficiaryIdentification = payment.getBeneficiaryIdentification();
        event.beneficiaryName = payment.getBeneficiaryName();
        event.amount = payment.getAmount();
        event.currency = payment.getCurrency();
        event.reference = payment.getReference();
        event.notificationEmail = payment.getNotificationEmail();
    }

    private OffUsPayment createPayment(PaymentLineRoutedOffUsEvent event) {
        OffsetDateTime now = OffsetDateTime.now();
        OffUsPayment payment = new OffUsPayment();
        payment.setId(UUID.randomUUID());
        payment.setBatchId(event.batchId);
        payment.setLineId(event.lineId);
        payment.setCorrelationId(event.correlationId);
        payment.setEventId(event.eventId);
        payment.setIdempotencyKey("OFFUS-" + event.lineId);
        payment.setCompanyRuc(event.companyRuc);
        payment.setSourceAccountNumber(event.sourceAccountNumber);
        payment.setCoreFundingId(event.coreFundingId);
        payment.setSequenceNumber(event.sequenceNumber);
        payment.setBeneficiaryIdentification(event.beneficiaryIdentification);
        payment.setBeneficiaryName(event.beneficiaryName);
        payment.setDestinationAccountNumber(event.destinationAccountNumber);
        payment.setRoutingCode(event.routingCode);
        payment.setDestinationInstitutionName(event.destinationInstitutionName);
        payment.setAmount(event.amount);
        payment.setCurrency(event.currency);
        payment.setReference(event.reference);
        payment.setNotificationEmail(event.notificationEmail);
        payment.setStatus(PaymentStatus.VALIDADA.name());
        payment.setCoreAccountingStatus(CoreAccountingStatus.PENDIENTE.name());
        payment.setFinalResultPublished(false);
        payment.setStatusQueryAttempts(0);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        return payment;
    }

    private ExternalPaymentRequest toExternalRequest(OffUsPayment payment) {
        return new ExternalPaymentRequest(
                payment.getLineId(),
                originBankCode,
                payment.getId().toString(),
                payment.getDestinationAccountNumber(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getReference(),
                payment.getBeneficiaryName(),
                LocalDate.now());
    }

    private OffUsPaymentAttempt startAttempt(OffUsPayment payment, AttemptOperationType type, Object request) {
        OffUsPaymentAttempt attempt = new OffUsPaymentAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setExternalPaymentOperationId(payment.getId());
        attempt.setAttemptNumber(attemptRepository.countByExternalPaymentOperationId(payment.getId()).intValue() + 1);
        attempt.setOperationType(type.name());
        attempt.setRequestPayloadSanitized(sanitizer.sanitize(request));
        attempt.setStartedAt(OffsetDateTime.now());
        attempt.setSuccessful(false);
        return attemptRepository.save(attempt);
    }

    private void finishAttempt(OffUsPaymentAttempt attempt, boolean success, Integer httpStatus, String externalStatus,
                               String errorCode, String errorMessage, Object response) {
        attempt.setSuccessful(success);
        attempt.setHttpStatus(httpStatus);
        attempt.setExternalStatus(externalStatus);
        attempt.setErrorCode(limit(errorCode, 80));
        attempt.setErrorMessage(limit(errorMessage, 500));
        attempt.setResponsePayloadSanitized(sanitizer.sanitize(response));
        attempt.setFinishedAt(OffsetDateTime.now());
        attemptRepository.save(attempt);
    }

    private void scheduleNextQuery(OffUsPayment payment) {
        payment.setNextStatusQueryAt(OffsetDateTime.now().plusNanos(pollIntervalMs * 1_000_000));
        payment.setUpdatedAt(OffsetDateTime.now());
    }

    private void validate(PaymentLineRoutedOffUsEvent event) {
        if (event == null || event.eventId == null || event.batchId == null || event.lineId == null || event.correlationId == null) {
            throw new IllegalArgumentException("PaymentLineRoutedOffUsEvent debe incluir eventId, batchId, lineId y correlationId.");
        }
        if (blank(event.coreFundingId) || blank(event.sourceAccountNumber) || blank(event.destinationAccountNumber)
                || blank(event.routingCode) || blank(event.currency) || event.amount == null
                || event.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("PaymentLineRoutedOffUsEvent no contiene los datos minimos para pago Off-Us.");
        }
    }

    private String mapDestinationBankCode(String routingCode) {
        return switch (routingCode) {
            case "30" -> "PICH";
            case "32" -> "GYQL";
            case "35" -> "PACF";
            default -> routingCode;
        };
    }

    private String userSafeMessage(FailureStage stage, String message) {
        if (stage == FailureStage.CORE_ACCOUNTING) {
            return "El pago externo fue procesado, pero no pudo registrarse contablemente en el mock del Core.";
        }
        return "El pago externo no pudo ser procesado.";
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String limit(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
