package com.patiperro.paseador.model;

/**
 * Estado del flujo de verificación de documentos de identidad del paseador
 * (distinto de la verificación de saldo en pagos-service).
 */
public enum EstadoVerificacionIdentidad {
    SIN_ENVIAR,
    EN_PROCESO,
    APROBADO,
    RECHAZADO
}
