package com.patiperro.reserva.service;

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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Orquesta devolución Mercado Pago (pagos-service) y aviso al tutor (notification-service).
 * <p>Orden: validar estado y cobro → llamada pagos → solo si {@code 204} persiste marca en reserva → correo tutor.
 * Ejecutado de forma asíncrona tras commit para no bloquear el hilo que cerró la transacción.</p>
 */
@Service
public class ReservaReembolsoService {

    private static final Logger log = LoggerFactory.getLogger(ReservaReembolsoService.class);

    private final ReservaRepository reservaRepository;
    private final PagosReembolsoIntegracionClient pagosReembolsoIntegracionClient;
    private final NotificacionReembolsoIntegracionClient notificacionReembolsoIntegracionClient;
    private final TutorIntegracionClient tutorIntegracionClient;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public ReservaReembolsoService(
            ReservaRepository reservaRepository,
            PagosReembolsoIntegracionClient pagosReembolsoIntegracionClient,
            NotificacionReembolsoIntegracionClient notificacionReembolsoIntegracionClient,
            TutorIntegracionClient tutorIntegracionClient,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.reservaRepository = reservaRepository;
        this.pagosReembolsoIntegracionClient = pagosReembolsoIntegracionClient;
        this.notificacionReembolsoIntegracionClient = notificacionReembolsoIntegracionClient;
        this.tutorIntegracionClient = tutorIntegracionClient;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Value("${patiperro.reserva.reembolso.retry-delay-ms:400}")
    private long retryDelayMs;

    @Value("${patiperro.reserva.reembolso.retry-max-extra-attempts:1}")
    private int retryMaxExtraAttempts;

    /**
     * Punto de entrada tras {@code afterCommit}: procesamiento en hilo async.
     */
    @Async
    public void procesarReembolsoYNotificar(Integer idReserva) {
        procesarReembolsoYNotificarSync(idReserva);
    }

    /**
     * Versión síncrona útil para pruebas o jobs que no deben usar async.
     */
    public void procesarReembolsoYNotificarSync(Integer idReserva) {
        if (idReserva == null) {
            return;
        }
        Reserva r = reservaRepository.findById(idReserva).orElse(null);
        if (r == null) {
            log.warn("Reembolso: reserva {} no encontrada", idReserva);
            return;
        }
        if (r.getMercadopagoReembolsoProcesadoEn() != null) {
            log.info("Reembolso: idempotente, ya marcado en reserva {} (procesadoEn={})", idReserva,
                    r.getMercadopagoReembolsoProcesadoEn());
            return;
        }

        EstadoReserva est = r.getEstadoReserva();
        if (!requiereReembolsoPorEstado(est)) {
            log.debug("Reembolso: reserva {} estado {} no amerita devolución MP", idReserva,
                    est != null ? est.getNombreEstado() : null);
            return;
        }

        String mpId = safe(r.getMercadopagoPaymentId());
        if (!StringUtils.hasText(mpId)) {
            log.debug("Reembolso: reserva {} sin mercadopago_payment_id; no se llama a pagos-service", idReserva);
            return;
        }

        if (!pagosReembolsoIntegracionClient.isEnabled()) {
            log.warn("Reembolso: integración pagos-reembolso deshabilitada o sin URL/secreto; omitido (reserva={})",
                    idReserva);
            return;
        }

        int code = solicitarReembolsoConReintento(idReserva, mpId);

        if (code == 204) {
            if (marcarReembolsoProcesado(idReserva)) {
                enviarNotificacionReembolsoAlTutor(idReserva, r.getIdTutorUsuario());
            } else {
                log.info("Reembolso: pagos OK pero marca procesadoEn ya existía; omitiendo notificación duplicada "
                        + "(reserva={})", idReserva);
            }
            return;
        }

        if (code == 409) {
            log.warn("Reembolso: pagos-service indica conflicto de negocio (no reembolsable o estado MP) "
                    + "(reserva={}, mpPaymentId={})", idReserva, mpId);
            return;
        }
        if (code == 400) {
            log.warn("Reembolso: solicitud inválida hacia pagos-service (reserva={}, mpPaymentId={})", idReserva, mpId);
            return;
        }
        if (code == 0) {
            log.warn("Reembolso: no hubo llamada HTTP a pagos-service (integración deshabilitada o sin cliente); "
                    + "sin marcar idempotencia ni notificar tutor (reserva={}, mpPaymentId={})", idReserva, mpId);
            return;
        }
        log.warn("Reembolso: pagos-service respondió {}; sin marcar idempotencia ni notificar tutor "
                + "(reserva={}, mpPaymentId={})", code, idReserva, mpId);
    }

    /**
     * Visible para tests del paquete de servicio.
     */
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
     * @return {@code true} si esta ejecución aplicó la marca (fila actualizada); {@code false} si ya estaba marcada.
     */
    private boolean marcarReembolsoProcesado(Integer idReserva) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        Integer filas = transactionTemplate.execute(
                status -> reservaRepository.marcarMercadopagoReembolsoProcesadoSiPendiente(idReserva, ahora));
        return filas != null && filas > 0;
    }

    /**
     * Reintento solo del correo (reembolso ya marcado). Usado por el scheduler de notificaciones at-least-once.
     */
    public void reintentarNotificacionReembolsoTutorSync(Integer idReserva) {
        if (idReserva == null) {
            return;
        }
        Reserva r = reservaRepository.findById(idReserva).orElse(null);
        if (r == null) {
            log.warn("Notificación reembolso (job): reserva {} no encontrada", idReserva);
            return;
        }
        if (r.getMercadopagoReembolsoProcesadoEn() == null) {
            return;
        }
        if (r.getNotificacionReembolsoEnviadaEn() != null) {
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
            LocalDateTime ahora = LocalDateTime.now(clock);
            Integer filas = transactionTemplate.execute(
                    status -> reservaRepository.marcarNotificacionReembolsoEnviadaSiPendiente(idReserva, ahora));
            if (filas != null && filas > 0) {
                log.info("Notificación reembolso tutor confirmada y persistida (reserva={}, correoPresente={})",
                        idReserva, StringUtils.hasText(correo));
            }
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

    private static String safe(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
