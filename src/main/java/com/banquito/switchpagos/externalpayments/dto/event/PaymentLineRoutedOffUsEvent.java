package com.banquito.switchpagos.externalpayments.dto.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class PaymentLineRoutedOffUsEvent {
    public UUID eventId;
    public String eventType;
    public OffsetDateTime occurredAt;
    public UUID batchId;
    public UUID lineId;
    public UUID correlationId;
    public String sourceService;
    public Integer sequenceNumber;
    public String companyRuc;
    public String sourceAccountNumber;
    public String coreFundingId;
    public String beneficiaryIdentification;
    public String beneficiaryName;
    public String destinationAccountNumber;
    public String routingCode;
    public String destinationInstitutionName;
    public BigDecimal amount;
    public String currency;
    public String reference;
    public String notificationEmail;
}
