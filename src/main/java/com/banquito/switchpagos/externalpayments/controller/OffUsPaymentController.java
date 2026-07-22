package com.banquito.switchpagos.externalpayments.controller;

import com.banquito.switchpagos.externalpayments.model.OffUsPayment;
import com.banquito.switchpagos.externalpayments.model.OffUsPaymentAttempt;
import com.banquito.switchpagos.externalpayments.repository.OffUsPaymentAttemptRepository;
import com.banquito.switchpagos.externalpayments.repository.OffUsPaymentRepository;
import com.banquito.switchpagos.externalpayments.service.OffUsPaymentService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/off-us-payments")
public class OffUsPaymentController {
    private final OffUsPaymentRepository paymentRepository;
    private final OffUsPaymentAttemptRepository attemptRepository;
    private final OffUsPaymentService paymentService;

    public OffUsPaymentController(OffUsPaymentRepository paymentRepository,
                                  OffUsPaymentAttemptRepository attemptRepository,
                                  OffUsPaymentService paymentService) {
        this.paymentRepository = paymentRepository;
        this.attemptRepository = attemptRepository;
        this.paymentService = paymentService;
    }

    @GetMapping("/{id}")
    public OffUsPayment getById(@PathVariable UUID id) {
        return paymentRepository.findById(id).orElseThrow();
    }

    @GetMapping("/by-line/{lineId}")
    public OffUsPayment getByLine(@PathVariable UUID lineId) {
        return paymentRepository.findByLineId(lineId).orElseThrow();
    }

    @GetMapping("/by-batch/{batchId}")
    public List<OffUsPayment> getByBatch(@PathVariable UUID batchId) {
        return paymentRepository.findByBatchIdOrderBySequenceNumberAsc(batchId);
    }

    @GetMapping("/{id}/attempts")
    public List<OffUsPaymentAttempt> getAttempts(@PathVariable UUID id) {
        return attemptRepository.findByExternalPaymentOperationIdOrderByAttemptNumberAsc(id);
    }

    @PostMapping("/{id}/resume")
    public OffUsPayment resume(@PathVariable UUID id) {
        return paymentService.resume(id);
    }
}
