package com.patiperro.pagos.reserva.dto;

import java.util.List;

/** Espejo JSON de {@code InternoBilleteraDetallesRequest} en reserva-service. */
public record ReservaInternoBilleteraDetallesRequest(Long idUsuarioPaseador, List<Integer> idsReserva) {
}
