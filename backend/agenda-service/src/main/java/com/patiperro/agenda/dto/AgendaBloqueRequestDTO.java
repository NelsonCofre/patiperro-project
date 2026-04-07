package com.patiperro.agenda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendaBloqueRequestDTO {

    @NotNull
    private Integer idUsuario;

    @NotNull
    private LocalDateTime horaInicio;

    @NotNull
    private LocalDateTime horaFinal;

    @NotNull
    private LocalDate fecha;

    @NotNull
    @Valid
    private EstadoBloqueRefDTO estadoBloque;

    @NotNull
    @Valid
    private DiaSemanaRefDTO diaSemana;
}
