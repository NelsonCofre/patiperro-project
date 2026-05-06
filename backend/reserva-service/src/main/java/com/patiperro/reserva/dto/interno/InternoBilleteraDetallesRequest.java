package com.patiperro.reserva.dto.interno;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Cuerpo de {@code POST /api/reserva/interno/billetera/detalles-paseador} (pagos-service).
 */
public record InternoBilleteraDetallesRequest(
        @NotNull Long idUsuarioPaseador,
        @NotEmpty @Size(max = 200) List<Integer> idsReserva
) {
}
