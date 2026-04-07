package com.patiperro.agenda.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaSemanaResponseDTO {

    private Integer idDia;
    private String nombre;
}
