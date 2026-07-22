package com.banquito.switchpagos.externalpayments.enums;

public enum PaymentStatus {
    RECIBIDA,
    VALIDADA,
    ENVIANDO_BANCO,
    PENDIENTE_BANCO,
    CONSULTANDO_BANCO,
    PROCESADA_BANCO,
    RECHAZADA_BANCO,
    FALLIDA_BANCO,
    REGISTRANDO_CORE,
    PROCESADA,
    FALLIDA_CORE,
    ERROR_TECNICO
}
