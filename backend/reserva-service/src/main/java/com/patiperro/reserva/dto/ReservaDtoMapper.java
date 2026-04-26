package com.patiperro.reserva.dto;

import com.patiperro.reserva.dto.integracion.AgendaBloqueReservaClientDTO;
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
        return toReservaResponse(r, null);
    }

    /**
     * @param bloque datos de agenda-service; si es null, los campos {@code agenda*} y {@code idPaseadorUsuario} quedan null.
     */
    public static ReservaResponseDTO toReservaResponse(Reserva r, AgendaBloqueReservaClientDTO bloque) {
        EstadoReserva est = r.getEstadoReserva();
        AgendaBloqueReservaClientDTO b = bloque;
        return new ReservaResponseDTO(
                r.getIdReserva(),
                r.getIdTutorUsuario(),
                r.getIdMascota(),
                r.getIdAgendaBloque(),
                r.getIdTarifa(),
                r.getFechaSolicitud(),
                r.getFechaAceptacion(),
                r.getMontoTotal(),
                r.getIdPago(),
                est != null ? est.getIdEstadoReserva() : null,
                est != null ? est.getNombreEstado() : null,
                r.getFechaInicioReal(),
                r.getFechaFin(),
                r.getCodigoEncuentro(),
                r.getCodigoEncuentroExpiraEn(),
                r.getMotivoRechazo(),
                r.getDetalleRechazo(),
                b != null ? b.getHoraInicio() : null,
                b != null ? b.getHoraFinal() : null,
                b != null ? b.getIdUsuario() : null);
    }
}
