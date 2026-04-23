package com.patiperro.reserva.event;

/**
 * Evento de dominio: inicio de paseo confirmado vía PIN (reserva pasa a {@code EN_CURSO} y se fija
 * {@code fecha_inicio_real}). Los efectos colaterales (STOMP, integraciones) se enganchan en
 * {@link PaseoIniciadoEventListener} tras el commit.
 */
public record PaseoIniciadoDomainEvent(Integer idReserva, String rawJwtPaseador) {}
