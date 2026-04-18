package com.patiperro.paseador.dto.agenda;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Forma del JSON devuelto por agenda-service en {@code GET /api/agenda/bloques/usuario/{id}/oferta}.
 */
@Data
public class AgendaBloqueOfertaJsonDTO {

    private Integer idAgenda;
    private Integer idUsuario;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFinal;
    private LocalDate fecha;
    private EstadoBloqueJsonDTO estadoBloque;
    private DiaSemanaJsonDTO diaSemana;

    @Data
    public static class EstadoBloqueJsonDTO {
        private Integer idEstado;
        private String nombre;
    }

    @Data
    public static class DiaSemanaJsonDTO {
        private Integer idDia;
        private String nombre;
    }
}
