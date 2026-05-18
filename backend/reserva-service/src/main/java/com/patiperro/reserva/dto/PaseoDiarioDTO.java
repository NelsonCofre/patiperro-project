package com.patiperro.reserva.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Vista mínima para el panel "Mis paseos de hoy" del paseador: evita enviar el payload
 * completo de {@link ReservaPaseadorSolicitudResponseDTO}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaseoDiarioDTO {

    private Integer idReserva;
    private Integer idAgendaBloque;

    private String mascotaNombre;

    /** Fecha del bloque (ISO-8601 fecha, ej. {@code 2026-05-13}). */
    private String fechaAgenda;
    /** Hora inicio del bloque, formato HH:mm. */
    private String horaInicio;
    /** Hora fin del bloque, formato HH:mm. */
    private String horaFin;

    /**
     * Inicio/fin programados del bloque tal como entrega agenda-service (útil para ordenar
     * o comparar con "ahora" sin parsear strings).
     */
    private LocalDateTime inicioProgramado;
    private LocalDateTime finProgramado;

    private String comuna;
    private String direccionReferencia;

    private Integer idEstadoReserva;
    private String nombreEstado;
}
