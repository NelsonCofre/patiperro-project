package com.patiperro.reserva.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadoReservaResponseDTO {

    private Integer idEstadoReserva;
    private String nombreEstado;
}
