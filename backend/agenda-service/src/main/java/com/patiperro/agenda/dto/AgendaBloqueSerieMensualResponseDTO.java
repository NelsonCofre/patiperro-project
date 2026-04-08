package com.patiperro.agenda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgendaBloqueSerieMensualResponseDTO {

    private int creados;
    private int omitidosPasado;
    private int omitidosSolape;
    private List<AgendaBloqueResponseDTO> bloques;
}
