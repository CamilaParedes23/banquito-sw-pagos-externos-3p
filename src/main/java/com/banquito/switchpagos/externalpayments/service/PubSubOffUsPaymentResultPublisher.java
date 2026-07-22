package com.banquito.switchpagos.externalpayments.service;

import com.banquito.switchpagos.externalpayments.dto.event.OffUsPaymentCompletedEvent;
import com.banquito.switchpagos.externalpayments.dto.event.OffUsPaymentFailedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "pubsub")
public class PubSubOffUsPaymentResultPublisher implements OffUsPaymentResultPublisher {

    private final ObjectMapper objectMapper;
    private final String projectId;
    private final String topicName;
    private final String schemaVersion;
    private Publisher publisher;

    public PubSubOffUsPaymentResultPublisher(
            ObjectMapper objectMapper,
            @Value("${pubsub.project-id}") String projectId,
            @Value("${pubsub.topic.external-payment-results}") String topicName,
            @Value("${pubsub.schema-version}") String schemaVersion) {
        this.objectMapper = objectMapper;
        this.projectId = projectId;
        this.topicName = topicName;
        this.schemaVersion = schemaVersion;
    }

    @PostConstruct
    public void start() throws Exception {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException("GOOGLE_CLOUD_PROJECT es obligatorio cuando MESSAGING_PROVIDER=pubsub");
        }
        publisher = Publisher.newBuilder(ProjectTopicName.of(projectId, topicName)).build();
    }

    @PreDestroy
    public void stop() throws Exception {
        if (publisher != null) {
            publisher.shutdown();
            publisher.awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    @Override
    public void publishCompleted(OffUsPaymentCompletedEvent event) {
        publish("OFF_US_COMPLETED", event, event.batchId, event.lineId, event.correlationId);
    }

    @Override
    public void publishFailed(OffUsPaymentFailedEvent event) {
        publish("OFF_US_FAILED", event, event.batchId, event.lineId, event.correlationId);
    }

    private void publish(String eventType, Object event, UUID batchId, UUID lineId, UUID correlationId) {
        try {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("eventType", eventType);
            attributes.put("sourceService", "external-payments-service");
            attributes.put("schemaVersion", schemaVersion);
            attributes.put("correlationId", correlationId.toString());
            attributes.put("batchId", batchId.toString());
            attributes.put("lineId", lineId.toString());
            PubsubMessage message = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFromUtf8(objectMapper.writeValueAsString(event)))
                    .putAllAttributes(attributes)
                    .build();
            publisher.publish(message).get(30, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo publicar " + eventType + " en Pub/Sub", ex);
        }
    }
}
