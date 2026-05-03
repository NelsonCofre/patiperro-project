package com.patiperro.reserva.dto;

import com.patiperro.reserva.dto.integracion.AgendaBloqueReservaClientDTO;
import com.patiperro.reserva.dto.integracion.TutorReservaClientDTO;
import com.patiperro.reserva.model.EstadoReserva;
import com.patiperro.reserva.model.EstadoReservaCatalogo;
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
        Integer idEstado = est != null ? est.getIdEstadoReserva() : null;
        Boolean puedeReintentarPago = EstadoReservaCatalogo.estadoAdmiteCheckoutOReintentoMercadoPago(idEstado);
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
                r.getMercadopagoPaymentId(),
                idEstado,
                est != null ? est.getNombreEstado() : null,
                r.getFechaInicioReal(),
                r.getFechaFin(),
                r.getCodigoEncuentro(),
                r.getCodigoEncuentroExpiraEn(),
                r.getMotivoRechazo(),
                r.getDetalleRechazo(),
                b != null ? b.getHoraInicio() : null,
                b != null ? b.getHoraFinal() : null,
                b != null ? b.getIdUsuario() : null,
                puedeReintentarPago,
                r.getMercadopagoUltimoEstado(),
                r.getMercadopagoUltimoEstadoDetalle(),
                r.getMercadopagoUltimoEstadoEn());
    }

    /** Detalle tutor; bloque/mascota/paseador/tutor vienen de integración externa. */
    public static ReservaTutorDetalleResponseDTO toTutorDetalleResponse(
            Reserva r,
            AgendaBloqueResumenDTO bloque,
            MascotaResumenDTO mascota,
            PaseadorResumenDTO paseador,
            TutorReservaClientDTO tutor,
            boolean puedeReintentarPago) {
        EstadoReserva estado = r.getEstadoReserva();
        String nombreTutorUi = nombreTutorParaDetalle(tutor, r.getIdTutorUsuario());
        String correoTutorUi = tutor != null ? tutor.getCorreo() : "sin-correo@patiperro.cl";
        return new ReservaTutorDetalleResponseDTO(
                r.getIdReserva(),
                r.getIdTutorUsuario(),
                r.getIdMascota(),
                mascota != null ? mascota.getNombre() : "Mascota #" + r.getIdMascota(),
                r.getIdAgendaBloque(),
                bloque != null ? bloque.getIdUsuario() : null,
                paseador != null ? paseador.getNombreCompleto() : "Paseador",
                bloque != null ? bloque.getFecha() : null,
                bloque != null ? bloque.getHoraInicio() : null,
                bloque != null ? bloque.getHoraFinal() : null,
                r.getMontoTotal(),
                r.getIdPago(),
                r.getMercadopagoPaymentId(),
                estado != null ? estado.getIdEstadoReserva() : null,
                estado != null ? estado.getNombreEstado() : null,
                r.getFechaSolicitud(),
                r.getFechaAceptacion(),
                r.getFechaInicioReal(),
                r.getFechaFin(),
                r.getCodigoEncuentro(),
                nombreTutorUi,
                correoTutorUi,
                r.getCodigoEncuentroExpiraEn(),
                r.getMotivoRechazo(),
                r.getDetalleRechazo(),
                r.getMercadopagoUltimoEstado(),
                r.getMercadopagoUltimoEstadoDetalle(),
                r.getMercadopagoUltimoEstadoEn(),
                puedeReintentarPago);
    }

    private static String nombreTutorParaDetalle(TutorReservaClientDTO tutor, Integer idTutorUsuario) {
        if (tutor == null) {
            return "Tutor #" + idTutorUsuario;
        }
        String ap = tutor.getApellidoPaterno() != null ? tutor.getApellidoPaterno() : "";
        String nombre = (tutor.getPrimerNombre() != null ? tutor.getPrimerNombre() : "") + " " + ap;
        return nombre.trim();
    }
}
