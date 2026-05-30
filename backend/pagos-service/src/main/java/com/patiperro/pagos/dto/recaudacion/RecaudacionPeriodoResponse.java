package com.patiperro.pagos.dto.recaudacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resumen de comisiones agrupadas. periodo representa el inicio del dia o mes solicitado.
 */
public record RecaudacionPeriodoResponse(
        LocalDateTime periodo,
        BigDecimal totalComision,
        Long totalEventos) {
}
