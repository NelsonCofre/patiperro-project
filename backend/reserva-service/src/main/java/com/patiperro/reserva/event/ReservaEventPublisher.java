package com.patiperro.reserva.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Punto único para publicar eventos de dominio de reservas (evita esparcir
 * {@link ApplicationEventPublisher} por servicios).
 */
@Component
@RequiredArgsConstructor
public class ReservaEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publicarPaseoIniciado(Integer idReserva, String rawJwtPaseador) {
        applicationEventPublisher.publishEvent(new PaseoIniciadoDomainEvent(idReserva, rawJwtPaseador));
    }
}
