package com.patiperro.reserva.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AgendaBloqueResumenDTO {
    private Integer idAgenda;
    private Integer idUsuario;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFinal;
    private LocalDate fecha;
}
