package com.patiperro.agenda.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendaBloqueoDiaRequestDTO {

    @NotNull
    @Positive
    private Integer idUsuario;

    @NotNull
    private LocalDate fecha;

    @Size(max = 120)
    private String motivo;
}
