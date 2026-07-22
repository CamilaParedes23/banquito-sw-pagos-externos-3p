package com.banquito.switchpagos.externalpayments.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "\"PAGO_EXTERNO_OFF_US\"")
public class OffUsPayment {

    @Id
    @Column(name = "\"ID_PAGO_EXTERNO_OFF_US\"", nullable = false)
    private UUID id;

    @Column(name = "\"ID_LOTE\"", nullable = false)
    private UUID batchId;
    @Column(name = "\"ID_LINEA\"", nullable = false, unique = true)
    private UUID lineId;
    @Column(name = "\"ID_CORRELACION\"", nullable = false)
    private UUID correlationId;
    @Column(name = "\"ID_EVENTO_ORIGEN\"", nullable = false)
    private UUID eventId;
    @Column(name = "\"IDEMPOTENCY_KEY\"", nullable = false, unique = true)
    private String idempotencyKey;
    @Column(name = "\"RUC_EMPRESA\"", nullable = false)
    private String companyRuc;
    @Column(name = "\"CUENTA_ORIGEN\"", nullable = false)
    private String sourceAccountNumber;
    @Column(name = "\"ID_FONDEO_CORE\"", nullable = false)
    private String coreFundingId;
    @Column(name = "\"SECUENCIAL\"", nullable = false)
    private Integer sequenceNumber;
    @Column(name = "\"IDENTIFICACION_BENEFICIARIO\"", nullable = false)
    private String beneficiaryIdentification;
    @Column(name = "\"NOMBRE_BENEFICIARIO\"", nullable = false)
    private String beneficiaryName;
    @Column(name = "\"CUENTA_DESTINO\"", nullable = false)
    private String destinationAccountNumber;
    @Column(name = "\"ROUTING_CODE\"", nullable = false)
    private String routingCode;
    @Column(name = "\"NOMBRE_INSTITUCION_DESTINO\"")
    private String destinationInstitutionName;
    @Column(name = "\"MONTO\"", nullable = false)
    private BigDecimal amount;
    @Column(name = "\"MONEDA\"", nullable = false)
    private String currency;
    @Column(name = "\"REFERENCIA\"")
    private String reference;
    @Column(name = "\"EMAIL_NOTIFICACION\"")
    private String notificationEmail;
    @Column(name = "\"ESTADO\"", nullable = false)
    private String status;
    @Column(name = "\"ID_PAGO_EXTERNO\"")
    private String externalPaymentId;
    @Column(name = "\"ESTADO_EXTERNO\"")
    private String externalStatus;
    @Column(name = "\"CODIGO_FALLO_EXTERNO\"")
    private String externalFailureCode;
    @Column(name = "\"MENSAJE_EXTERNO\"")
    private String externalMessage;
    @Column(name = "\"FECHA_PROCESADO_EXTERNO\"")
    private OffsetDateTime externalProcessedAt;
    @Column(name = "\"ESTADO_CONTABLE_CORE\"")
    private String coreAccountingStatus;
    @Column(name = "\"ID_TRANSACCION_CORE\"")
    private String coreTransactionId;
    @Column(name = "\"RESULTADO_FINAL_PUBLICADO\"", nullable = false)
    private Boolean finalResultPublished;
    @Column(name = "\"INTENTOS_CONSULTA_ESTADO\"", nullable = false)
    private Integer statusQueryAttempts;
    @Column(name = "\"PROXIMA_CONSULTA_ESTADO\"")
    private OffsetDateTime nextStatusQueryAt;
    @Column(name = "\"FECHA_CREACION\"", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "\"FECHA_ACTUALIZACION\"", nullable = false)
    private OffsetDateTime updatedAt;
    @Column(name = "\"FECHA_FINALIZACION\"")
    private OffsetDateTime completedAt;
    @Version
    @Column(name = "\"VERSION\"", nullable = false)
    private Long version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBatchId() { return batchId; }
    public void setBatchId(UUID batchId) { this.batchId = batchId; }
    public UUID getLineId() { return lineId; }
    public void setLineId(UUID lineId) { this.lineId = lineId; }
    public UUID getCorrelationId() { return correlationId; }
    public void setCorrelationId(UUID correlationId) { this.correlationId = correlationId; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getCompanyRuc() { return companyRuc; }
    public void setCompanyRuc(String companyRuc) { this.companyRuc = companyRuc; }
    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public void setSourceAccountNumber(String sourceAccountNumber) { this.sourceAccountNumber = sourceAccountNumber; }
    public String getCoreFundingId() { return coreFundingId; }
    public void setCoreFundingId(String coreFundingId) { this.coreFundingId = coreFundingId; }
    public Integer getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Integer sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    public String getBeneficiaryIdentification() { return beneficiaryIdentification; }
    public void setBeneficiaryIdentification(String beneficiaryIdentification) { this.beneficiaryIdentification = beneficiaryIdentification; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }
    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public void setDestinationAccountNumber(String destinationAccountNumber) { this.destinationAccountNumber = destinationAccountNumber; }
    public String getRoutingCode() { return routingCode; }
    public void setRoutingCode(String routingCode) { this.routingCode = routingCode; }
    public String getDestinationInstitutionName() { return destinationInstitutionName; }
    public void setDestinationInstitutionName(String destinationInstitutionName) { this.destinationInstitutionName = destinationInstitutionName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getNotificationEmail() { return notificationEmail; }
    public void setNotificationEmail(String notificationEmail) { this.notificationEmail = notificationEmail; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getExternalPaymentId() { return externalPaymentId; }
    public void setExternalPaymentId(String externalPaymentId) { this.externalPaymentId = externalPaymentId; }
    public String getExternalStatus() { return externalStatus; }
    public void setExternalStatus(String externalStatus) { this.externalStatus = externalStatus; }
    public String getExternalFailureCode() { return externalFailureCode; }
    public void setExternalFailureCode(String externalFailureCode) { this.externalFailureCode = externalFailureCode; }
    public String getExternalMessage() { return externalMessage; }
    public void setExternalMessage(String externalMessage) { this.externalMessage = externalMessage; }
    public OffsetDateTime getExternalProcessedAt() { return externalProcessedAt; }
    public void setExternalProcessedAt(OffsetDateTime externalProcessedAt) { this.externalProcessedAt = externalProcessedAt; }
    public String getCoreAccountingStatus() { return coreAccountingStatus; }
    public void setCoreAccountingStatus(String coreAccountingStatus) { this.coreAccountingStatus = coreAccountingStatus; }
    public String getCoreTransactionId() { return coreTransactionId; }
    public void setCoreTransactionId(String coreTransactionId) { this.coreTransactionId = coreTransactionId; }
    public Boolean getFinalResultPublished() { return finalResultPublished; }
    public void setFinalResultPublished(Boolean finalResultPublished) { this.finalResultPublished = finalResultPublished; }
    public Integer getStatusQueryAttempts() { return statusQueryAttempts; }
    public void setStatusQueryAttempts(Integer statusQueryAttempts) { this.statusQueryAttempts = statusQueryAttempts; }
    public OffsetDateTime getNextStatusQueryAt() { return nextStatusQueryAt; }
    public void setNextStatusQueryAt(OffsetDateTime nextStatusQueryAt) { this.nextStatusQueryAt = nextStatusQueryAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
