package com.patiperro.pagos.dto.billetera;

import java.math.BigDecimal;
import java.time.Instant;

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
        String nombreEstadoReserva,
        /**
         * Cobro del tutor en pagos-service ({@code transaccion.id_transaccion}); conciliación con el mismo ID que la
         * auditoría de liberación (opción A). Null solo si el ítem no tiene tracking de cobro asociado.
         */
        Long idTransaccionPagos,
        /**
         * Inicio del día calendario N+2 (zona {@code patiperro.pagos.billetera.zona}) en que el monto puede pasar a
         * retirable; solo aplica en verificación. Null en retenido, sin {@code fecha_fin_servicio}, o ya liberado.
         * Una disputa activa puede posponer la liberación efectiva aunque esta fecha ya haya pasado.
         */
        Instant disponibleParaRetiroEn
) {
}
