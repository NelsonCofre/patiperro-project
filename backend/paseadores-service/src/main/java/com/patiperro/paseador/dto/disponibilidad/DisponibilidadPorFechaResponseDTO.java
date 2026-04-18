package com.patiperro.paseador.dto.disponibilidad;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** Bloques disponibles de un paseador para una fecha concreta. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadPorFechaResponseDTO {

    private LocalDate fecha;
    private List<BloqueDisponibleResponseDTO> bloques;
}
