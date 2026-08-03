package com.banquito.switchpagos.externalpayments.listener;

import com.banquito.switchpagos.externalpayments.dto.event.PaymentLineRoutedOffUsEvent;
import com.banquito.switchpagos.externalpayments.service.OffUsPaymentService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "rabbitmq")
public class PaymentLineRoutedOffUsListener {
    private final OffUsPaymentService offUsPaymentService;

    public PaymentLineRoutedOffUsListener(OffUsPaymentService offUsPaymentService) {
        this.offUsPaymentService = offUsPaymentService;
    }

    @RabbitListener(queues = "${rabbit.queue.external-payments.off-us}")
    public void handle(PaymentLineRoutedOffUsEvent event) {
        offUsPaymentService.processRoutedOffUsLine(event);
    }
}
