package com.banquito.switchpagos.externalpayments.scheduler;

import com.banquito.switchpagos.externalpayments.service.OffUsPaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PendingPaymentScheduler {
    private final OffUsPaymentService offUsPaymentService;
    private final Boolean enabled;

    public PendingPaymentScheduler(OffUsPaymentService offUsPaymentService,
                                   @Value("${external.payment.poll.enabled}") Boolean enabled) {
        this.offUsPaymentService = offUsPaymentService;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${external.payment.poll.interval-ms}", initialDelayString = "${external.payment.poll.initial-delay-ms}")
    public void pollPendingPayments() {
        if (Boolean.TRUE.equals(enabled)) {
            offUsPaymentService.pollPendingPayments();
        }
    }
}
