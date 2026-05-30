package com.patiperro.pagos.dto.comprobante;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ComprobantePagoResponse(
        String tipoDocumento,
        String disclaimerLegal,
        Long idReserva,
        Long idOrden,
        String idTransaccionExterna,
        LocalDateTime fechaHoraOperacion,
        String paseadorNombre,
        String mascotaNombre,
        LocalDate fechaPaseo,
        LocalDateTime horaInicio,
        LocalDateTime horaFinal,
        Long duracionMinutos,
        String moneda,
        BigDecimal montoTotal,
        BigDecimal comisionApp,
        BigDecimal montoNeto,
        String estadoFondos
) {
}

