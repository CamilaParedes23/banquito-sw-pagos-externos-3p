package com.banquito.switchpagos.externalpayments.client;

import com.banquito.switchpagos.externalpayments.dto.interbank.InterbankPaymentRequest;
import com.banquito.switchpagos.externalpayments.dto.interbank.InterbankPaymentResponse;
import java.util.UUID;

public interface ExternalBankPaymentClient {
    InterbankPaymentResponse createPayment(String idempotencyKey, InterbankPaymentRequest request);
    InterbankPaymentResponse getByPaymentLineId(UUID paymentLineId);
}
