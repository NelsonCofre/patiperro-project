package com.patiperro.pagos.dto.billetera;

import java.time.LocalDateTime;
import java.util.List;

public record BilleteraResumenPaseadorResponse(
        BilleteraBucketResponse retenido,
        BilleteraBucketResponse verificacion,
        BilleteraBucketResponse disponible,
        /**
         * Movimientos de liberación N+2 ya aplicados (por reserva). La suma de estos montos puede no coincidir con
         * {@code disponible.amount} por retiros, reembolsos u otros ajustes sobre {@code saldo_actual}.
         */
        List<BilleteraReservaItemResponse> historialLiberacionesDisponible,
        LocalDateTime updatedAt
) {
}
