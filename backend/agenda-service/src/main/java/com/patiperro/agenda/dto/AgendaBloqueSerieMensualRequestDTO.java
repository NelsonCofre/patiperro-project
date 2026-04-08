package com.patiperro.agenda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Crea un bloque por cada ocurrencia del mismo día de la semana que {@code fechaSemilla}
 * dentro de su mes/año, excluyendo fechas anteriores a hoy (zona horaria del servidor).
 * Las horas se toman de {@code horaInicio} / {@code horaFinal} (solo la parte horaria).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendaBloqueSerieMensualRequestDTO {

    @NotNull
    private Integer idUsuario;

    /** Define mes, año y día de la semana a repetir. */
    @NotNull
    private LocalDate fechaSemilla;

    /** Debe ser el mismo día calendario que fechaSemilla; se usa la hora de inicio. */
    @NotNull
    private LocalDateTime horaInicio;

    @NotNull
    private LocalDateTime horaFinal;

    @NotNull
    @Valid
    private EstadoBloqueRefDTO estadoBloque;
}
