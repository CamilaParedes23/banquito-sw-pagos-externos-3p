package com.banquito.switchpagos.externalpayments.listener;

import com.banquito.switchpagos.externalpayments.dto.event.PaymentLineRoutedOffUsEvent;
import com.banquito.switchpagos.externalpayments.service.OffUsPaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "pubsub")
public class PubSubPaymentLineRoutedOffUsSubscriber {

    private static final Logger LOG = LoggerFactory.getLogger(PubSubPaymentLineRoutedOffUsSubscriber.class);

    private final ObjectMapper objectMapper;
    private final OffUsPaymentService offUsPaymentService;
    private final String projectId;
    private final String subscriptionName;
    private Subscriber subscriber;

    public PubSubPaymentLineRoutedOffUsSubscriber(
            ObjectMapper objectMapper,
            OffUsPaymentService offUsPaymentService,
            @Value("${pubsub.project-id}") String projectId,
            @Value("${pubsub.subscription.external-payments-off-us}") String subscriptionName) {
        this.objectMapper = objectMapper;
        this.offUsPaymentService = offUsPaymentService;
        this.projectId = projectId;
        this.subscriptionName = subscriptionName;
    }

    @PostConstruct
    public void start() {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException("GOOGLE_CLOUD_PROJECT es obligatorio cuando MESSAGING_PROVIDER=pubsub");
        }
        MessageReceiver receiver = this::receive;
        subscriber = Subscriber.newBuilder(ProjectSubscriptionName.of(projectId, subscriptionName), receiver).build();
        subscriber.startAsync().awaitRunning();
    }

    @PreDestroy
    public void stop() throws Exception {
        if (subscriber != null) {
            subscriber.stopAsync().awaitTerminated(30, TimeUnit.SECONDS);
        }
    }

    private void receive(PubsubMessage message, AckReplyConsumer consumer) {
        try {
            PaymentLineRoutedOffUsEvent event = objectMapper.readValue(message.getData().toStringUtf8(), PaymentLineRoutedOffUsEvent.class);
            offUsPaymentService.processRoutedOffUsLine(event);
            consumer.ack();
        } catch (Exception ex) {
            LOG.error("Error procesando PAYMENT_LINE_OFF_US desde Pub/Sub. messageId={}", message.getMessageId(), ex);
            consumer.nack();
        }
    }
}
