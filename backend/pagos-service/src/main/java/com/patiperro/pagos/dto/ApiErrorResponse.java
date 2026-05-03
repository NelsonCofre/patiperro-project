package com.patiperro.pagos.dto;

/**
 * Cuerpo JSON estable para errores de las APIs propias de pagos-service (internas o futuras públicas).
 */
public record ApiErrorResponse(String codigo, String mensaje, boolean reintentable) {

    public static ApiErrorResponse servicioNoDisponible() {
        return new ApiErrorResponse(
                "SERVICIO_NO_DISPONIBLE",
                "Integración interna no configurada.",
                false);
    }

    public static ApiErrorResponse accesoDenegado() {
        return new ApiErrorResponse(
                "ACCESO_DENEGADO",
                "Cabecera interna inválida.",
                false);
    }

    public static ApiErrorResponse solicitudInvalida(String mensaje) {
        return new ApiErrorResponse("SOLICITUD_INVALIDA", mensaje, false);
    }

    public static ApiErrorResponse checkoutNoDisponible() {
        return new ApiErrorResponse(
                "CHECKOUT_NO_DISPONIBLE",
                "No se pudo crear la preferencia de pago. Revise token y URLs de checkout de Mercado Pago.",
                true);
    }
}
