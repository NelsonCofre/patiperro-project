package com.patiperro.reserva.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Cuerpo de {@code PATCH /api/reserva/{id}/status}: una sola acción por petición.
 */
@Data
public class BookingStatusPatchRequestDTO {

    /** Decisión del paseador (aceptar, rechazar, iniciar o finalizar). */
    private PaseadorDecision decision;

    /** Alternativa explícita al enum {@link #decision} (mismo uso que en integraciones legacy). */
    @Positive
    private Integer idEstadoReserva;

    /** Acción del tutor (p. ej. anular solicitud pendiente). */
    private TutorDecision tutorDecision;

    /** Texto corto opcional cuando el paseador rechaza. */
    private String motivoRechazo;

    /** Texto libre opcional para explicar el rechazo. */
    private String detalleRechazo;

    @AssertTrue(message = "Debe enviar exactamente uno: decision, idEstadoReserva o tutorDecision")
    public boolean isCuerpoValido() {
        int n = 0;
        if (decision != null) {
            n++;
        }
        if (idEstadoReserva != null) {
            n++;
        }
        if (tutorDecision != null) {
            n++;
        }
        return n == 1;
    }

    /**
     * Acciones del tutor sobre el estado de la reserva (PATCH {@code /api/reserva/{id}/status}).
     */
    public enum TutorDecision {
        /** Cancela la solicitud mientras la reserva está en SOLICITADA. */
        CANCELAR_SOLICITUD
    }
}
