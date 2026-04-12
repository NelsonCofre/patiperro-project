package com.patiperro.agenda.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class WalkerBlackoutRequestDTO {

    @NotNull
    @Positive
    private Integer idUsuario;

    @NotNull
    private LocalDate fechaInicio;

    /** Si es null, tratar como un solo día (= fechaInicio). */
    private LocalDate fechaFin;

    @Size(max = 120)
    private String motivo;
}