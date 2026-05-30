package com.patiperro.pagos.dto.checkout;

import java.math.BigDecimal;

/**
 * Solicitud interna para crear una preferencia Checkout Pro.
 */
public record CrearPreferenciaRequest(Integer idReserva, BigDecimal montoTotal, String tituloItem) {
}
