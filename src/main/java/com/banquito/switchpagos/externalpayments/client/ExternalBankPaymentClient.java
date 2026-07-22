package com.banquito.switchpagos.externalpayments.client;

import com.banquito.switchpagos.externalpayments.dto.mock.ExternalPaymentRequest;
import com.banquito.switchpagos.externalpayments.dto.mock.ExternalPaymentResponse;

public interface ExternalBankPaymentClient {
    ExternalPaymentResponse createPayment(String idempotencyKey, ExternalPaymentRequest request);
    ExternalPaymentResponse getByExternalPaymentId(String externalPaymentId);
    ExternalPaymentResponse getByIdempotencyKey(String idempotencyKey);
}
