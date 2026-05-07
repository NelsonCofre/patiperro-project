package com.patiperro.pagos.reserva.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservaComprobanteDto(
        Integer idReserva,
        Integer idTutorUsuario,
        String tutorCorreo,
        Integer idMascota,
        String mascotaNombre,
        Integer idPaseadorUsuario,
        String paseadorNombre,
        Integer idAgendaBloque,
        LocalDate fechaPaseo,
        LocalDateTime horaInicio,
        LocalDateTime horaFinal,
        BigDecimal montoTotal,
        Long idTransaccionPagos) {
}

