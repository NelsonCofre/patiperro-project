package com.patiperro.pagos.dto.billetera;

import java.math.BigDecimal;
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
        /**
         * Tasa configurada en el momento de la consulta (fracción 0–1, coherente con {@code patiperro.pagos.comision.plataforma-tasa}).
         * No reemplaza el desglose por ítem: cobros antiguos pueden haberse liquidado con otra tasa si la política cambió.
         */
        BigDecimal tasaComisionPlataforma,
        /**
         * Misma política expresada como porcentaje 0–100 (dos decimales) para textos de UI (ej. {@code 5.00} = 5%).
         */
        BigDecimal porcentajeComisionPlataforma,
        LocalDateTime updatedAt
) {
}
