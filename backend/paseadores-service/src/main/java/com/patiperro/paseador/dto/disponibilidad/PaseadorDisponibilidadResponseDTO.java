package com.patiperro.paseador.dto.disponibilidad;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Respuesta de disponibilidad pública: ventana de N días agrupada por fecha. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaseadorDisponibilidadResponseDTO {

    private Long idPaseador;
    private int dias;
    private List<DisponibilidadPorFechaResponseDTO> porFecha;
}
