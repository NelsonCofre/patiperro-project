package com.patiperro.pagos.reserva.dto;

import java.math.BigDecimal;

public record ReservaConsultaDto(
        Long idReserva,
        Long idTutorUsuario,
        BigDecimal montoTotal,
        String mercadopagoPaymentId) {
}
