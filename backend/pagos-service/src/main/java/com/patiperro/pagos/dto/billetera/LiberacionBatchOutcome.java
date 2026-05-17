package com.patiperro.pagos.dto.billetera;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado de una corrida de liberación masiva (scheduler). Logs y aviso opcional consolidado al paseador.
 */
public record LiberacionBatchOutcome(int totalReservasLiberadas, List<LiberacionLineaPaseador> porPaseador) {

    public static LiberacionBatchOutcome vacio() {
        return new LiberacionBatchOutcome(0, List.of());
    }

    /** Una fila por paseador con montos netos sumados y cantidad de reservas liberadas en esta corrida. */
    public record LiberacionLineaPaseador(Long idUsuarioPaseador, BigDecimal montoNetoTotal, int cantidadReservas) {}
}
