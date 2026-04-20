package com.patiperro.reserva.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Línea de tiempo de una reserva para UI de seguimiento.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingTimelineResponseDTO {

    private Integer idReserva;
    private Integer idTutorUsuario;
    private Integer idMascota;
    private Integer idAgendaBloque;
    private Integer idEstadoReservaActual;
    private String nombreEstadoActual;
    private List<TimelineStepDTO> steps;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineStepDTO {
        private String key;
        private String label;
        private boolean complete;
        private LocalDateTime changedAt;
    }
}

