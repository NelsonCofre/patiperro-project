package com.patiperro.reserva.service;

import com.patiperro.reserva.dto.PaseadorResumenDTO;
import com.patiperro.reserva.dto.integracion.AgendaBloqueReservaClientDTO;
import com.patiperro.reserva.model.EstadoReserva;
import com.patiperro.reserva.model.EstadoReservaCatalogo;
import com.patiperro.reserva.model.Reserva;
import com.patiperro.reserva.repository.ReservaRepository;
import com.patiperro.reserva.support.AgendaIntegracionClient;
import com.patiperro.reserva.support.NotificacionPagoIntegracionClient;
import com.patiperro.reserva.support.PagosBilleteraIntegracionClient;
import com.patiperro.reserva.support.PaseadorIntegracionClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Lógica de negocio para aplicar efectos de pago en la reserva.
 * <p>Orden de trabajo sugerido (solo backend), paso 1: al aprobar cobro, persistir {@code mercadopago_payment_id}
 * en la entidad {@link Reserva} (requisito para reembolsos vía {@link ReservaReembolsoService}).</p>
 */
@Service
@RequiredArgsConstructor
public class ReservaPagoService {

    private static final Logger log = LoggerFactory.getLogger(ReservaPagoService.class);

    private final ReservaRepository reservaRepository;
    private final EstadoReservaService estadoReservaService;
    private final AgendaIntegracionClient agendaIntegracionClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificacionPagoIntegracionClient notificacionPagoIntegracionClient;
    private final PaseadorIntegracionClient paseadorIntegracionClient;
    private final PagosBilleteraIntegracionClient pagosBilleteraIntegracionClient;

    /**
     * Persiste el enlace a la transacción en pagos-service (checkout iniciado desde pagos-service).
     */
    @Transactional
    public void vincularTransaccionPagos(Integer idReserva, Long idTransaccionPagos) {
        if (idReserva == null || idTransaccionPagos == null) {
            throw new IllegalArgumentException("idReserva e idTransaccionPagos son obligatorios");
        }
        Reserva r = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
        if (r.getIdPago() != null && !r.getIdPago().equals(idTransaccionPagos)) {
            log.warn("Vincular transacción: reserva ya tenía otro id_pago; no se sobrescribe (idReserva={})", idReserva);
            return;
        }
        r.setIdPago(idTransaccionPagos);
        reservaRepository.save(r);
    }

    /**
     * Idempotente: si ya está PAGADA, no repite STOMP/notificación/billetera; aún persiste {@code mpPaymentId}
     * si faltaba y reintenta sincronizar agenda vía interno tras commit.
     *
     * @param idTransaccionPagos {@code transaccion.id_transaccion} en pagos-service
     * @param mpPaymentId        opcional; si viene, se guarda en {@code mercadopago_payment_id} (reembolsos / soporte)
     */
    @Transactional
    public void marcarReservaComoPagada(Integer idReserva, Long idTransaccionPagos, String mpPaymentId) {
        if (idReserva == null || idTransaccionPagos == null) {
            throw new IllegalArgumentException("idReserva e idTransaccionPagos son obligatorios");
        }
        Reserva r = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        EstadoReserva actual = r.getEstadoReserva();
        String mpId = safe(mpPaymentId);
        if (actual != null) {
            Integer idEstado = actual.getIdEstadoReserva();
            String nombre = actual.getNombreEstado();
            boolean yaPagada = (idEstado != null && idEstado.equals(EstadoReservaCatalogo.ID_PAGADA))
                    || (nombre != null && nombre.trim().equalsIgnoreCase(EstadoReservaCatalogo.NOMBRE_PAGADA));
            if (yaPagada) {
                log.info("Pago aprobado idempotente: reserva ya estaba PAGADA (idReserva={}, idTransaccion={}, mp={})",
                        idReserva, idTransaccionPagos, mpId);
                boolean needSave = false;
                if (r.getIdPago() == null) {
                    r.setIdPago(idTransaccionPagos);
                    needSave = true;
                } else if (!r.getIdPago().equals(idTransaccionPagos)) {
                    log.warn("Pago aprobado idempotente: reserva ya tenía otro id_pago; no se sobrescribe (idReserva={})",
                            idReserva);
                }
                String mpPersistId = truncate(mpId, MP_PAYMENT_ID_MAX);
                if (mpPersistId != null && !StringUtils.hasText(r.getMercadopagoPaymentId())) {
                    r.setMercadopagoPaymentId(mpPersistId);
                    needSave = true;
                }
                if (needSave) {
                    reservaRepository.save(r);
                }
                programarSincroniaExternaTrasCommitPago(r.getIdAgendaBloque(), false, null, null, null);
                return;
            }
        }

        EstadoReserva pagada = estadoReservaService.obtenerPorNombreIgnoreCase(EstadoReservaCatalogo.NOMBRE_PAGADA);
        Reserva rUpdate = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
        EstadoReserva estadoActualUpd = rUpdate.getEstadoReserva();
        Integer idEstadoUpd = estadoActualUpd != null ? estadoActualUpd.getIdEstadoReserva() : null;
        if (!EstadoReservaCatalogo.estadoAdmiteMarcarPagadaPorCobroMercadoPago(idEstadoUpd)) {
            String estado = estadoActualUpd != null ? estadoActualUpd.getNombreEstado() : "SIN_ESTADO";
            log.warn(
                    "Cobro MP aprobado no aplicado a reserva: estado no admite PAGADA (idReserva={}, idEstado={}, nombre={})",
                    idReserva, idEstadoUpd, estado);
            throw new IllegalStateException(
                    "No se puede marcar PAGADA desde estado actual: "
                            + estado
                            + ". Solo se permiten orígenes SOLICITADA, PENDIENTE_PAGO o ACEPTADA "
                            + "(p. ej. webhook tardío tras expiración/cancelación queda rechazado).");
        }
        rUpdate.setEstadoReserva(pagada);
        rUpdate.setIdPago(idTransaccionPagos);
        String mpPersistId = truncate(mpId, MP_PAYMENT_ID_MAX);
        if (mpPersistId != null) {
            rUpdate.setMercadopagoPaymentId(mpPersistId);
        }
        Reserva saved = reservaRepository.saveAndFlush(rUpdate);

        Integer idPaseador = null;
        try {
            if (agendaIntegracionClient.isEnabled()) {
                AgendaBloqueReservaClientDTO bloque = agendaIntegracionClient.obtenerBloquePorIdInterno(saved.getIdAgendaBloque());
                idPaseador = bloque != null ? bloque.getIdUsuario() : null;
            }
        } catch (RuntimeException e) {
            log.warn("Pago aprobado: no se pudo resolver idPaseador desde agenda-service (idReserva={}, idAgendaBloque={})",
                    idReserva, saved.getIdAgendaBloque(), e);
        }

        PagoConfirmadoEventDTO evt = new PagoConfirmadoEventDTO(
                "PAGO_CONFIRMADO",
                saved.getIdReserva(),
                saved.getIdTutorUsuario(),
                idPaseador,
                saved.getIdPago(),
                mpId
        );
        messagingTemplate.convertAndSend("/topic/reservas/" + saved.getIdReserva() + "/pagos", evt);
        if (saved.getIdTutorUsuario() != null) {
            messagingTemplate.convertAndSend("/topic/tutor/" + saved.getIdTutorUsuario() + "/pagos", evt);
        }
        if (idPaseador != null) {
            messagingTemplate.convertAndSend("/topic/paseador/" + idPaseador + "/pagos", evt);
        }

        if (notificacionPagoIntegracionClient.isEnabled() && idPaseador != null) {
            try {
                PaseadorResumenDTO resumen = paseadorIntegracionClient.obtenerResumen(idPaseador);
                String correo = resumen != null ? resumen.getCorreo() : null;
                if (!notificacionPagoIntegracionClient.notificarPagoConfirmado(saved.getIdReserva(), idPaseador, correo)) {
                    log.warn("Pago aprobado: no se completó la notificación al notification-service "
                            + "(idReserva={}, idPaseador={})", saved.getIdReserva(), idPaseador);
                }
            } catch (RuntimeException e) {
                log.warn("Pago aprobado: no se pudo notificar por correo (idReserva={}, idPaseador={})",
                        saved.getIdReserva(), idPaseador, e);
            }
        }

        programarSincroniaExternaTrasCommitPago(
                saved.getIdAgendaBloque(), true, saved.getIdReserva(), saved.getIdPago(), idPaseador);

        log.info("Pago aprobado aplicado: reserva marcada PAGADA (idReserva={}, idTutor={}, idPaseador={}, idTransaccion={}, mp={})",
                saved.getIdReserva(), saved.getIdTutorUsuario(), idPaseador, idTransaccionPagos, mpId);
    }

    private static final int MP_ESTADO_MAX = 32;
    private static final int MP_DETALLE_MAX = 120;
    private static final int MP_PAYMENT_ID_MAX = 64;

    /**
     * Tras COMMIT de la reserva: acreditación billetera (solo primera vez PAGADA) y sincronía agenda (siempre best-effort).
     */
    private void programarSincroniaExternaTrasCommitPago(
            Integer idAgendaBloque,
            boolean acreditarBilletera,
            Integer idReservaWallet,
            Long idTransaccionWallet,
            Integer idUsuarioWalker) {
        Runnable task =
                () -> {
                    if (acreditarBilletera
                            && pagosBilleteraIntegracionClient.isEnabled()
                            && idUsuarioWalker != null
                            && idTransaccionWallet != null
                            && idReservaWallet != null) {
                        pagosBilleteraIntegracionClient.acreditarRetenido(
                                idReservaWallet, idUsuarioWalker.longValue(), idTransaccionWallet);
                    }
                    sincronizarBloqueAgendaReservadoTrasPago(idAgendaBloque);
                };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            task.run();
                        }
                    });
        } else {
            task.run();
        }
    }

    private void sincronizarBloqueAgendaReservadoTrasPago(Integer idAgendaBloque) {
        if (!agendaIntegracionClient.isEnabled() || idAgendaBloque == null) {
            return;
        }
        try {
            agendaIntegracionClient.marcarBloqueReservadoInterno(idAgendaBloque);
            log.debug("Sincronía post-pago: bloque {} marcado reservado en agenda-service", idAgendaBloque);
        } catch (RuntimeException e) {
            log.warn(
                    "Sincronía post-pago: no se pudo marcar bloque {} como reservado en agenda (reserva sigue PAGADA)",
                    idAgendaBloque,
                    e);
        }
    }

    /**
     * {@code true} si el tutor puede volver a intentar el pago en pasarela (misma regla que registro de fallo MP).
     */
    public boolean permiteReintentarPago(Reserva r) {
        if (r == null || r.getEstadoReserva() == null) {
            return false;
        }
        Integer id = r.getEstadoReserva().getIdEstadoReserva();
        return EstadoReservaCatalogo.estadoAdmiteCheckoutOReintentoMercadoPago(id);
    }

    /**
     * Persiste el último estado MP cuando el cobro no está aprobado (rechazo, cancelación, etc.).
     * <ul>
     *   <li>Idempotente si la reserva ya está {@code PAGADA}.</li>
     *   <li>Solo aplica en estados que admiten checkout/reintento ({@link EstadoReservaCatalogo#estadoAdmiteCheckoutOReintentoMercadoPago}).</li>
     *   <li>Si llega el mismo {@code status} y {@code status_detail} ya persistidos, no reescribe.</li>
     * </ul>
     */
    @Transactional
    public void registrarMercadoPagoNoAprobado(Integer idReserva, String mpPaymentId, String mpStatus, String mpStatusDetail) {
        if (idReserva == null) {
            throw new IllegalArgumentException("idReserva es obligatorio");
        }
        if (!StringUtils.hasText(mpPaymentId)) {
            throw new IllegalArgumentException("mpPaymentId es obligatorio");
        }
        if (!StringUtils.hasText(mpStatus)) {
            throw new IllegalArgumentException("mpStatus es obligatorio");
        }
        Reserva r = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        EstadoReserva actual = r.getEstadoReserva();
        if (actual != null && actual.getIdEstadoReserva() != null
                && actual.getIdEstadoReserva().equals(EstadoReservaCatalogo.ID_PAGADA)) {
            log.info("MP no aprobado ignorado: reserva ya PAGADA (idReserva={}, mpPaymentId={}, status={})",
                    idReserva, mpPaymentId.trim(), mpStatus.trim());
            return;
        }

        if (actual == null || actual.getIdEstadoReserva() == null
                || !EstadoReservaCatalogo.estadoAdmiteCheckoutOReintentoMercadoPago(actual.getIdEstadoReserva())) {
            log.info("MP no aprobado ignorado: estado actual no admite registrar intento (idReserva={}, estadoId={}, status={})",
                    idReserva,
                    actual != null ? actual.getIdEstadoReserva() : null,
                    mpStatus.trim());
            return;
        }

        String estadoNuevo = truncate(mpStatus, MP_ESTADO_MAX);
        String detalleNuevo = truncate(mpStatusDetail, MP_DETALLE_MAX);
        if (Objects.equals(estadoNuevo, r.getMercadopagoUltimoEstado())
                && Objects.equals(detalleNuevo, r.getMercadopagoUltimoEstadoDetalle())) {
            log.debug("MP no aprobado idempotente: sin cambios (idReserva={}, status={})", idReserva, estadoNuevo);
            return;
        }

        r.setMercadopagoUltimoEstado(estadoNuevo);
        r.setMercadopagoUltimoEstadoDetalle(detalleNuevo);
        r.setMercadopagoUltimoEstadoEn(LocalDateTime.now());
        reservaRepository.save(r);
        log.info("Registrado intento MP no aprobado (idReserva={}, mpPaymentId={}, status={})",
                idReserva, mpPaymentId.trim(), estadoNuevo);
    }

    private static String truncate(String s, int maxLen) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String t = s.trim();
        return t.length() <= maxLen ? t : t.substring(0, maxLen);
    }

    public record PagoConfirmadoEventDTO(
            String type,
            Integer idReserva,
            Integer idTutorUsuario,
            Integer idPaseadorUsuario,
            Long idTransaccionPagos,
            String mpPaymentId
    ) {
    }

    private static String safe(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
