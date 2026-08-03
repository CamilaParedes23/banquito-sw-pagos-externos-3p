package com.banquito.switchpagos.externalpayments.client;

import com.banquito.switchpagos.externalpayments.dto.interbank.ErrorResponse;
import com.banquito.switchpagos.externalpayments.dto.interbank.InterbankPaymentRequest;
import com.banquito.switchpagos.externalpayments.dto.interbank.InterbankPaymentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(name = "external.bank.client.mode", havingValue = "real")
public class RealExternalBankPaymentClient implements ExternalBankPaymentClient {

    private final RestClient externalBankRestClient;
    private final ExternalBankTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;
    private final String paymentPath;

    public RealExternalBankPaymentClient(
            @Qualifier("externalBankRestClient") RestClient externalBankRestClient,
            ExternalBankTokenProvider tokenProvider,
            ObjectMapper objectMapper,
            @Value("${external.bank.payment-path}") String paymentPath) {
        this.externalBankRestClient = externalBankRestClient;
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
        this.paymentPath = paymentPath;
    }

    @Override
    public InterbankPaymentResponse createPayment(String idempotencyKey, InterbankPaymentRequest request) {
        try {
            return externalBankRestClient.post()
                    .uri(paymentPath)
                    .headers(headers -> applyPaymentHeaders(headers, idempotencyKey, request.correlationId()))
                    .body(request)
                    .retrieve()
                    .body(InterbankPaymentResponse.class);
        } catch (RestClientResponseException exception) {
            throw mapResponseException(exception);
        } catch (ResourceAccessException exception) {
            throw new ExternalBankTimeoutException("Timeout o error de conectividad al crear pago interbancario.");
        }
    }

    @Override
    public InterbankPaymentResponse getByPaymentLineId(UUID paymentLineId) {
        try {
            return externalBankRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(paymentPath)
                            .queryParam("paymentLineUuid", paymentLineId)
                            .build())
                    .headers(headers -> headers.setBearerAuth(tokenProvider.getBearerToken()))
                    .retrieve()
                    .body(InterbankPaymentResponse.class);
        } catch (RestClientResponseException exception) {
            throw mapResponseException(exception);
        } catch (ResourceAccessException exception) {
            throw new ExternalBankTimeoutException("Timeout o error de conectividad al consultar pago interbancario.");
        }
    }

    private void applyPaymentHeaders(HttpHeaders headers, String idempotencyKey, UUID correlationId) {
        headers.setBearerAuth(tokenProvider.getBearerToken());
        headers.set("Idempotency-Key", idempotencyKey);
        headers.set("X-Correlation-Id", correlationId.toString());
    }

    private ExternalBankClientException mapResponseException(RestClientResponseException exception) {
        String code = "EXTERNAL_BANK_HTTP_" + exception.getStatusCode().value();
        String message = "El banco externo rechazo la operacion. httpStatus=" + exception.getStatusCode().value();
        ErrorResponse error = parseError(exception.getResponseBodyAsString());
        if (error != null) {
            if (error.code() != null && !error.code().isBlank()) {
                code = error.code();
            }
            if (error.message() != null && !error.message().isBlank()) {
                message = error.message();
            }
        }
        if (exception.getStatusCode().value() == 409) {
            return new ExternalBankConflictException(message);
        }
        return new ExternalBankClientException(code, message, exception.getStatusCode().value());
    }

    private ErrorResponse parseError(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(body, ErrorResponse.class);
        } catch (Exception ignored) {
            return null;
        }
    }
}
