package com.patiperro.pagos.dto.billetera;

import java.math.BigDecimal;
import java.util.List;

/**
 * Un bucket de la billetera del paseador (retenido, verificación o disponible).
 * <p>
 * {@link #amount()} es el saldo <strong>neto</strong> agregado según columnas de {@code billetera} para ese estado.
 * {@link #grossAmount()} y {@link #commissionAmount()} son sumas del desglose de los ítems listados en {@link #reservas()}
 * (bruto y comisión total); sirven para transparencia junto al detalle por reserva. La tasa vigente del servidor no se
 * repite aquí: va en {@link BilleteraResumenPaseadorResponse#tasaComisionPlataforma()}.
 * </p>
 */
public record BilleteraBucketResponse(
        /** Clave estable ({@code retenido}, {@code verificacion}, {@code disponible}) para UI o rutas derivadas. */
        String key,
        String title,
        String helper,
        /**
         * Saldo neto del bucket: {@code saldo_retenido}, {@code saldo_verificacion} o {@code saldo_actual} según corresponda.
         */
        BigDecimal amount,
        /**
         * Suma de {@link BilleteraReservaItemResponse#montoBruto()} de {@link #reservas()}; {@code 0} si no hay ítems.
         */
        BigDecimal grossAmount,
        /**
         * Suma de {@link BilleteraReservaItemResponse#comision()} de {@link #reservas()}; {@code 0} si no hay ítems.
         */
        BigDecimal commissionAmount,
        /**
         * Detalle por reserva (bruto, comisión y neto por ítem). Vacío en disponible cuando no se listan movimientos activos.
         */
        List<BilleteraReservaItemResponse> reservas
) {
}
