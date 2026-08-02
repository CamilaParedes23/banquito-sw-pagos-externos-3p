package com.banquito.switchpagos.externalpayments.controller;

import com.banquito.switchpagos.externalpayments.client.ExternalBankPaymentClient;
import com.banquito.switchpagos.externalpayments.dto.mock.ExternalPaymentRequest;
import com.banquito.switchpagos.externalpayments.dto.mock.ExternalPaymentResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/interbank/payments")
public class MockExternalBankController {
    private final ExternalBankPaymentClient externalBankPaymentClient;

    public MockExternalBankController(ExternalBankPaymentClient externalBankPaymentClient) {
        this.externalBankPaymentClient = externalBankPaymentClient;
    }

    @PostMapping
    public ExternalPaymentResponse create(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                          @org.springframework.web.bind.annotation.RequestBody ExternalPaymentRequest request) {
        return externalBankPaymentClient.createPayment(idempotencyKey, request);
    }

    @GetMapping("/{externalPaymentId}")
    public ExternalPaymentResponse get(@PathVariable String externalPaymentId) {
        return externalBankPaymentClient.getByExternalPaymentId(externalPaymentId);
    }

    @GetMapping("/by-idempotency/{idempotencyKey}")
    public ExternalPaymentResponse getByIdempotency(@PathVariable String idempotencyKey) {
        return externalBankPaymentClient.getByIdempotencyKey(idempotencyKey);
    }
}
