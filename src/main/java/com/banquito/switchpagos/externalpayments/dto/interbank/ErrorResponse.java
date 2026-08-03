package com.banquito.switchpagos.externalpayments.dto.interbank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ErrorResponse(
        String timestamp,
        UUID correlationId,
        String code,
        String message,
        List<String> details) {
}
