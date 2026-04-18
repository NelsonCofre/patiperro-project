package com.patiperro.reserva.dto.integracion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Subconjunto del JSON de {@code GET /api/agenda/bloques/{id}} en agenda-service.
 * Ignora campos anidados no usados por reserva-service (p. ej. estadoBloque, diaSemana).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgendaBloqueReservaClientDTO {

    private Integer idAgenda;
    private Integer idUsuario;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFinal;
    private LocalDate fecha;
}
