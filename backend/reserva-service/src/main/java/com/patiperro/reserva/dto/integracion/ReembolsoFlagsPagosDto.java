package com.patiperro.reserva.dto.integracion;

/**
 * Respuesta de {@code GET /api/pagos/interno/reembolso/flags-reserva/{id}}.
 */
public record ReembolsoFlagsPagosDto(
        boolean tieneCobroAprobadoMp,
        boolean reembolsoMpRegistrado,
        boolean correoReembolsoEnviado) {
}
