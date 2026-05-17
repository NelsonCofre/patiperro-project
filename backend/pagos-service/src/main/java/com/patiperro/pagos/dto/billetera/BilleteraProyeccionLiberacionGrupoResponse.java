package com.patiperro.pagos.dto.billetera;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Agrupa reservas en verificación (N+1) por la fecha calendario en que, si no hay disputa activa,
 * el monto pasará a saldo disponible (misma regla que {@code BilleteraLiberacionTransaccionalService}: día N del fin
 * de servicio + 2 días).
 */
public record BilleteraProyeccionLiberacionGrupoResponse(
        LocalDate fechaDisponibleDesde,
        BigDecimal totalNeto,
        boolean liberacionPausadaPorDisputa,
        List<BilleteraReservaItemResponse> reservas
) {}
