package com.patiperro.pagos.dto.billetera;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ítem del historial de solicitudes de retiro del paseador (tabla {@code retiro_fondos} + estado en {@code transaccion}).
 */
public record RetiroHistorialItemResponse(
        Long idRetiroFondos,
        Long idTransaccion,
        String operationId,
        BigDecimal monto,
        String estadoPago,
        String estadoEtiqueta,
        LocalDateTime solicitadoEn,
        /** Resumen de la cuenta bancaria actual del paseador (la operación no guarda snapshot de cuenta). */
        String cuentaDestinoResumen
) {
}
