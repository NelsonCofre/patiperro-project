package com.patiperro.paseador.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta paginada para búsqueda pública de paseadores cercanos con conteo real de resultados
 * que cumplen los filtros (incl. disponibilidad si aplica).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaseadorCercanosConConteoResponseDTO {

    /** Total de paseadores que cumplen los filtros (sin aplicar paginación). */
    private int totalDisponibles;

    /** Paginación solicitada. */
    private int offset;
    private int limit;

    /** Resultados de la página actual. */
    private List<PaseadorCercanoResponseDTO> resultados;
}

