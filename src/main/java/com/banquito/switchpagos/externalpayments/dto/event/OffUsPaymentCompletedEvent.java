package com.banquito.switchpagos.externalpayments.dto.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class OffUsPaymentCompletedEvent {
    public UUID eventId;
    public String eventType;
    public OffsetDateTime occurredAt;
    public UUID batchId;
    public UUID lineId;
    public UUID correlationId;
    public Integer sequenceNumber;
    public String companyRuc;
    public String coreFundingId;
    public String routingCode;
    public String destinationInstitutionName;
    public String destinationAccountNumber;
    public String beneficiaryIdentification;
    public String beneficiaryName;
    public BigDecimal amount;
    public String currency;
    public String reference;
    public String notificationEmail;
    public String externalPaymentId;
    public OffsetDateTime externalProcessedAt;
    public String coreTransactionId;
    public String result;
    public Boolean billable;
}
