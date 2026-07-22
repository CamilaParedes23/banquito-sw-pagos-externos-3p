package com.banquito.switchpagos.externalpayments.service;

import com.banquito.switchpagos.externalpayments.dto.event.OffUsPaymentCompletedEvent;
import com.banquito.switchpagos.externalpayments.dto.event.OffUsPaymentFailedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitOffUsPaymentResultPublisher implements OffUsPaymentResultPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String completedRoutingKey;
    private final String failedRoutingKey;

    public RabbitOffUsPaymentResultPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${rabbit.exchange.external-payments}") String exchange,
            @Value("${rabbit.routing-key.off-us-completed}") String completedRoutingKey,
            @Value("${rabbit.routing-key.off-us-failed}") String failedRoutingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.completedRoutingKey = completedRoutingKey;
        this.failedRoutingKey = failedRoutingKey;
    }

    @Override
    public void publishCompleted(OffUsPaymentCompletedEvent event) {
        rabbitTemplate.convertAndSend(exchange, completedRoutingKey, event);
    }

    @Override
    public void publishFailed(OffUsPaymentFailedEvent event) {
        rabbitTemplate.convertAndSend(exchange, failedRoutingKey, event);
    }
}
