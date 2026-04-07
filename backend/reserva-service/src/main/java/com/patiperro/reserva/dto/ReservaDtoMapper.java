package com.patiperro.reserva.dto;

import com.patiperro.reserva.model.EstadoReserva;
import com.patiperro.reserva.model.Reserva;

public final class ReservaDtoMapper {

    private ReservaDtoMapper() {}

    public static EstadoReservaResponseDTO toEstadoResponse(EstadoReserva e) {
        if (e == null) {
            return null;
        }
        return new EstadoReservaResponseDTO(e.getIdEstadoReserva(), e.getNombreEstado());
    }

    public static ReservaResponseDTO toReservaResponse(Reserva r) {
        EstadoReserva est = r.getEstadoReserva();
        return new ReservaResponseDTO(
                r.getIdReserva(),
                r.getIdTutorUsuario(),
                r.getIdMascota(),
                r.getIdAgendaBloque(),
                r.getIdTarifa(),
                r.getFechaSolicitud(),
                r.getMontoTotal(),
                r.getIdPago(),
                est != null ? est.getIdEstadoReserva() : null,
                est != null ? est.getNombreEstado() : null,
                r.getFechaInicioReal(),
                r.getFechaFin(),
                r.getCodigoEncuentro());
    }
}
