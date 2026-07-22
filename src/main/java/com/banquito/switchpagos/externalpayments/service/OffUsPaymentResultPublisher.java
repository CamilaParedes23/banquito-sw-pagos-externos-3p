package com.banquito.switchpagos.externalpayments.service;

import com.banquito.switchpagos.externalpayments.dto.event.OffUsPaymentCompletedEvent;
import com.banquito.switchpagos.externalpayments.dto.event.OffUsPaymentFailedEvent;

public interface OffUsPaymentResultPublisher {
    void publishCompleted(OffUsPaymentCompletedEvent event);
    void publishFailed(OffUsPaymentFailedEvent event);
}
