package com.patiperro.reserva.event;

import lombok.Value;

/**
 * Se emite tras validar el PIN y persistir {@code EN_CURSO} + {@code fecha_inicio_real}
 * para ejecutar notificaciones/integración fuera de la transacción de reserva.
 */
@Value
public class PaseoIniciadoEvent {
    Integer idReserva;
    String rawJwtPaseador;
}
