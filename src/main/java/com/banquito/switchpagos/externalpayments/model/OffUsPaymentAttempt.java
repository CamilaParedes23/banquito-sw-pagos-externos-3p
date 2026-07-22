package com.banquito.switchpagos.externalpayments.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "\"INTENTO_PAGO_EXTERNO\"")
public class OffUsPaymentAttempt {
    @Id
    @Column(name = "\"ID_INTENTO_PAGO_EXTERNO\"", nullable = false)
    private UUID id;
    @Column(name = "\"ID_PAGO_EXTERNO_OFF_US\"", nullable = false)
    private UUID externalPaymentOperationId;
    @Column(name = "\"NUMERO_INTENTO\"", nullable = false)
    private Integer attemptNumber;
    @Column(name = "\"TIPO_OPERACION\"", nullable = false)
    private String operationType;
    @Column(name = "\"REQUEST_PAYLOAD_SANITIZED\"")
    private String requestPayloadSanitized;
    @Column(name = "\"RESPONSE_PAYLOAD_SANITIZED\"")
    private String responsePayloadSanitized;
    @Column(name = "\"HTTP_STATUS\"")
    private Integer httpStatus;
    @Column(name = "\"ESTADO_EXTERNO\"")
    private String externalStatus;
    @Column(name = "\"CODIGO_ERROR\"")
    private String errorCode;
    @Column(name = "\"MENSAJE_ERROR\"")
    private String errorMessage;
    @Column(name = "\"FECHA_INICIO\"", nullable = false)
    private OffsetDateTime startedAt;
    @Column(name = "\"FECHA_FIN\"")
    private OffsetDateTime finishedAt;
    @Column(name = "\"EXITOSO\"", nullable = false)
    private Boolean successful;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getExternalPaymentOperationId() { return externalPaymentOperationId; }
    public void setExternalPaymentOperationId(UUID externalPaymentOperationId) { this.externalPaymentOperationId = externalPaymentOperationId; }
    public Integer getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getRequestPayloadSanitized() { return requestPayloadSanitized; }
    public void setRequestPayloadSanitized(String requestPayloadSanitized) { this.requestPayloadSanitized = requestPayloadSanitized; }
    public String getResponsePayloadSanitized() { return responsePayloadSanitized; }
    public void setResponsePayloadSanitized(String responsePayloadSanitized) { this.responsePayloadSanitized = responsePayloadSanitized; }
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    public String getExternalStatus() { return externalStatus; }
    public void setExternalStatus(String externalStatus) { this.externalStatus = externalStatus; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(OffsetDateTime finishedAt) { this.finishedAt = finishedAt; }
    public Boolean getSuccessful() { return successful; }
    public void setSuccessful(Boolean successful) { this.successful = successful; }
}
