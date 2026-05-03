package com.patiperro.reserva.service;

import com.patiperro.reserva.dto.integracion.ReembolsoFlagsPagosDto;
import com.patiperro.reserva.model.EstadoReserva;
import com.patiperro.reserva.model.EstadoReservaCatalogo;
import com.patiperro.reserva.model.Reserva;
import com.patiperro.reserva.repository.ReservaRepository;
import com.patiperro.reserva.support.NotificacionReembolsoIntegracionClient;
import com.patiperro.reserva.support.PagosReembolsoIntegracionClient;
import com.patiperro.reserva.support.TutorIntegracionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Orquesta devolución Mercado Pago (pagos-service) y aviso al tutor (notification-service).
 * Estado de reembolso y correo se registra en {@code pago_externo} (pagos-service); la reserva solo enlaza {@code id_pago}.
 */
@Service
public class ReservaReembolsoService {

    private static final Logger log = LoggerFactory.getLogger(ReservaReembolsoService.class);

    private final ReservaRepository reservaRepository;
    private final PagosReembolsoIntegracionClient pagosReembolsoIntegracionClient;
    private final NotificacionReembolsoIntegracionClient notificacionReembolsoIntegracionClient;
    private final TutorIntegracionClient tutorIntegracionClient;

    public ReservaReembolsoService(
            ReservaRepository reservaRepository,
            PagosReembolsoIntegracionClient pagosReembolsoIntegracionClient,
            NotificacionReembolsoIntegracionClient notificacionReembolsoIntegracionClient,
            TutorIntegracionClient tutorIntegracionClient) {
        this.reservaRepository = reservaRepository;
        this.pagosReembolsoIntegracionClient = pagosReembolsoIntegracionClient;
        this.notificacionReembolsoIntegracionClient = notificacionReembolsoIntegracionClient;
        this.tutorIntegracionClient = tutorIntegracionClient;
    }

    @Value("${patiperro.reserva.reembolso.retry-delay-ms:400}")
    private long retryDelayMs;

    @Value("${patiperro.reserva.reembolso.retry-max-extra-attempts:1}")
    private int retryMaxExtraAttempts;

    @Async
    public void procesarReembolsoYNotificar(Integer idReserva) {
        procesarReembolsoYNotificarSync(idReserva);
    }

    public void procesarReembolsoYNotificarSync(Integer idReserva) {
        if (idReserva == null) {
            return;
        }
        Reserva r = reservaRepository.findById(idReserva).orElse(null);
        if (r == null) {
            log.warn("Reembolso: reserva {} no encontrada", idReserva);
            return;
        }
        if (!pagosReembolsoIntegracionClient.isEnabled()) {
            log.warn("Reembolso: integración pagos-reembolso deshabilitada o sin URL/secreto; omitido (reserva={})",
                    idReserva);
            return;
        }

        Optional<ReembolsoFlagsPagosDto> flagsOpt = pagosReembolsoIntegracionClient.consultarFlagsReembolso(idReserva);
        if (flagsOpt.map(ReembolsoFlagsPagosDto::correoReembolsoEnviado).orElse(false)) {
            return;
        }

        EstadoReserva est = r.getEstadoReserva();
        if (!requiereReembolsoPorEstado(est)) {
            log.debug("Reembolso: reserva {} estado {} no amerita devolución MP", idReserva,
                    est != null ? est.getNombreEstado() : null);
            return;
        }
        if (r.getIdPago() == null) {
            log.debug("Reembolso: reserva {} sin id_pago (enlace a transacción en pagos-service)", idReserva);
            return;
        }

        boolean tieneAprobado = flagsOpt.map(ReembolsoFlagsPagosDto::tieneCobroAprobadoMp).orElse(true);
        if (!tieneAprobado) {
            log.debug("Reembolso: reserva {} sin cobro aprobado en pagos-service", idReserva);
            return;
        }

        if (flagsOpt.map(ReembolsoFlagsPagosDto::reembolsoMpRegistrado).orElse(false)) {
            enviarNotificacionReembolsoAlTutor(idReserva, r.getIdTutorUsuario());
            return;
        }

        int code = solicitarReembolsoConReintento(idReserva, null);

        if (code == 204) {
            enviarNotificacionReembolsoAlTutor(idReserva, r.getIdTutorUsuario());
            return;
        }

        if (code == 409) {
            log.warn("Reembolso: pagos-service indica conflicto de negocio (no reembolsable o estado MP) "
                    + "(reserva={})", idReserva);
            return;
        }
        if (code == 400) {
            log.warn("Reembolso: solicitud inválida hacia pagos-service (reserva={})", idReserva);
            return;
        }
        if (code == 0) {
            log.warn("Reembolso: no hubo llamada HTTP a pagos-service; sin notificar tutor (reserva={})", idReserva);
            return;
        }
        log.warn("Reembolso: pagos-service respondió {}; sin notificar tutor (reserva={})", code, idReserva);
    }

    boolean requiereReembolsoPorEstado(EstadoReserva est) {
        return esRechazada(est) || esExpirada(est) || esCancelada(est);
    }

    private int solicitarReembolsoConReintento(Integer idReserva, String mpId) {
        int code = pagosReembolsoIntegracionClient.solicitarReembolsoTotal(idReserva, mpId);
        int extra = Math.max(0, retryMaxExtraAttempts);
        int intentos = 0;
        while (debeReintentarCodigoHttp(code) && intentos < extra) {
            dormirAntesDeReintento();
            code = pagosReembolsoIntegracionClient.solicitarReembolsoTotal(idReserva, mpId);
            intentos++;
        }
        return code;
    }

    private static boolean debeReintentarCodigoHttp(int code) {
        return code == 502 || code == 503 || code == 500 || code == 429;
    }

    private void dormirAntesDeReintento() {
        long ms = Math.max(0, retryDelayMs);
        if (ms == 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Reembolso: interrupción durante espera de reintento");
        }
    }

    /**
     * Reintento solo del correo (reembolso ya registrado en pagos). Usado por el scheduler at-least-once.
     */
    public void reintentarNotificacionReembolsoTutorSync(Integer idReserva) {
        if (idReserva == null) {
            return;
        }
        if (!pagosReembolsoIntegracionClient.isEnabled()) {
            return;
        }
        Optional<ReembolsoFlagsPagosDto> fo = pagosReembolsoIntegracionClient.consultarFlagsReembolso(idReserva);
        if (fo.isEmpty()) {
            return;
        }
        ReembolsoFlagsPagosDto f = fo.get();
        if (!f.reembolsoMpRegistrado() || f.correoReembolsoEnviado()) {
            return;
        }
        Reserva r = reservaRepository.findById(idReserva).orElse(null);
        if (r == null) {
            log.warn("Notificación reembolso (job): reserva {} no encontrada", idReserva);
            return;
        }
        enviarNotificacionReembolsoAlTutor(idReserva, r.getIdTutorUsuario());
    }

    private void enviarNotificacionReembolsoAlTutor(Integer idReserva, Integer idTutorUsuario) {
        if (!notificacionReembolsoIntegracionClient.isEnabled()) {
            log.warn("Reembolso: integración notificación-reembolso deshabilitada; tutor sin correo automático "
                    + "(reserva={})", idReserva);
            return;
        }
        String correo = idTutorUsuario != null
                ? tutorIntegracionClient.obtenerCorreoInterno(idTutorUsuario.longValue())
                : null;
        boolean ok = notificacionReembolsoIntegracionClient.notificarReembolsoTutor(idReserva, correo);
        if (ok) {
            pagosReembolsoIntegracionClient.marcarCorreoReembolsoEnviadoEnPagos(idReserva);
            log.info("Notificación reembolso tutor confirmada y persistida en pagos-service (reserva={}, correoPresente={})",
                    idReserva, StringUtils.hasText(correo));
        } else {
            log.warn("Notificación reembolso tutor fallida o no aplicada; quedará para job de reenvío (reserva={})",
                    idReserva);
        }
    }

    private static boolean esRechazada(EstadoReserva e) {
        return coincide(e, EstadoReservaCatalogo.ID_RECHAZADA, EstadoReservaCatalogo.NOMBRE_RECHAZADA);
    }

    private static boolean esExpirada(EstadoReserva e) {
        return coincide(e, EstadoReservaCatalogo.ID_EXPIRADA, EstadoReservaCatalogo.NOMBRE_EXPIRADA);
    }

    private static boolean esCancelada(EstadoReserva e) {
        return coincide(e, EstadoReservaCatalogo.ID_CANCELADA, EstadoReservaCatalogo.NOMBRE_CANCELADA);
    }

    private static boolean coincide(EstadoReserva e, int idEsperado, String nombreEsperado) {
        if (e == null) {
            return false;
        }
        Integer id = e.getIdEstadoReserva();
        if (id != null && id.equals(idEsperado)) {
            return true;
        }
        String n = e.getNombreEstado();
        return n != null && n.trim().equalsIgnoreCase(nombreEsperado);
    }
}
