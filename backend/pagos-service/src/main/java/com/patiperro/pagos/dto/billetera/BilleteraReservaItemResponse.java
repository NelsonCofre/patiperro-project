package com.patiperro.pagos.dto.billetera;

import java.math.BigDecimal;

public record BilleteraReservaItemResponse(
        Integer idReserva,
        BigDecimal montoBruto,
        BigDecimal comision,
        BigDecimal montoNeto,
        String estadoEtiqueta,
        /** Enriquecido vía reserva-service cuando la integración está activa; puede ser null. */
        String mascotaNombre,
        String tutorNombre,
        String fechaAgenda,
        String horaInicio,
        /** Nombre legible del estado de la reserva en dominio reserva (ej. PAGADA); puede ser null. */
        String nombreEstadoReserva
) {
}
