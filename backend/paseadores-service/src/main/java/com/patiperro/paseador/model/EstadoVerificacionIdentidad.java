package com.patiperro.paseador.model;

import java.util.Optional;

/**
 * Estado del flujo de verificación de documentos de identidad del paseador.
 * Persistido como {@code STRING} en {@code paseador.estado_verificacion_identidad}
 * (valores deben coincidir con el CHECK de Flyway).
 * <p>
 * Distinto de la verificación de saldo ({@code saldo_verificacion}) en pagos-service.
 */
public enum EstadoVerificacionIdentidad {

    SIN_ENVIAR("Sin enviar"),
    EN_PROCESO("Verificación en proceso"),
    APROBADO("Verificado"),
    RECHAZADO("Rechazado");

    private final String etiqueta;

    EstadoVerificacionIdentidad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Primera subida o reenvío tras rechazo. */
    public boolean puedeSubirDocumentos() {
        return this == SIN_ENVIAR || this == RECHAZADO;
    }

    /** Reemplazo del PDF ya verificado. */
    public boolean puedeCambiarDocumento() {
        return this == APROBADO;
    }

    /** POST /documento permitido salvo revisión admin pendiente. */
    public boolean puedeEnviarDocumento() {
        return this != EN_PROCESO;
    }

    public boolean esAprobado() {
        return this == APROBADO;
    }

    /** Estados válidos en {@code PUT /interno/{id}/verificacion-identidad}. */
    public boolean esDecisionRevisionAdmin() {
        return this == APROBADO || this == RECHAZADO;
    }

    public Optional<String> mensajeBloqueoSubida() {
        return switch (this) {
            case EN_PROCESO -> Optional.of(
                    "Verificación en proceso: no puedes subir nuevos documentos hasta que un administrador responda");
            default -> Optional.empty();
        };
    }
}
