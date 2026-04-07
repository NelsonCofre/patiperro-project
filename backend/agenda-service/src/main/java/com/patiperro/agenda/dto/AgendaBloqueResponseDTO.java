package com.patiperro.agenda.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendaBloqueResponseDTO {

    private Integer idAgenda;
    private Integer idUsuario;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFinal;
    private LocalDate fecha;
    private EstadoBloqueResponseDTO estadoBloque;
    private DiaSemanaResponseDTO diaSemana;
}
