package com.patiperro.reserva.dto;

import java.math.BigDecimal;

/**
 * Datos expuestos a pagos-service (interno) para armar Checkout Pro.
 */
public record ReservaParaPagoDto(
        Long idReserva,
        Long idTutorUsuario,
        BigDecimal montoTotal,
        /** {@code transaccion.id_transaccion} en pagos-service; null si aún no se inició checkout. */
        Long idTransaccionPagos) {
}
