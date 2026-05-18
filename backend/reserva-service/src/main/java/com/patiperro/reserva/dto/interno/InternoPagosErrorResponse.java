package com.patiperro.reserva.dto.interno;

/**
 * Cuerpo JSON para errores en endpoints internos de pagos (consumidos por pagos-service).
 */
public record InternoPagosErrorResponse(String codigo, String mensaje) {
}
