package com.patiperro.agenda.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendaBloqueoDiaResponseDTO {

    private Integer idBloqueo;
    private Integer idUsuario;
    private LocalDate fecha;
    private String motivo;
    private LocalDateTime creadoEn;
}
