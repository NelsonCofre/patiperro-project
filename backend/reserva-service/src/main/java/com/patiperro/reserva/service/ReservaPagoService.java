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

import java.util.Objects;

/** Lógica de negocio para aplicar efectos de pago en la reserva. */
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
    private final ReservaComprobantePostCommitRunner reservaComprobantePostCommitRunner;

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
     * Idempotente: si ya está PAGADA, no repite STOMP/notificación/billetera y reintenta sincronizar agenda tras commit.
     *
     * @param idTransaccionPagos {@code transaccion.id_transaccion} en pagos-service
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
                if (needSave) {
                    reservaRepository.save(r);
                }
                programarSincroniaExternaTrasCommitPago(r.getIdAgendaBloque(), false, null, null, null);
                programarComprobantePostCommit(idReserva);
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

        // Retención en billetera del paseador: solo si ya había aceptado (pago después de aceptar).
        // Si estaba SOLICITADA/PENDIENTE_PAGO, el acreditado se hace al aceptar (ReservaService).
        boolean acreditarRetenidoAhora = Objects.equals(EstadoReservaCatalogo.ID_ACEPTADA, idEstadoUpd);
        programarSincroniaExternaTrasCommitPago(
                saved.getIdAgendaBloque(), acreditarRetenidoAhora, saved.getIdReserva(), saved.getIdPago(), idPaseador);
        programarComprobantePostCommit(saved.getIdReserva());

        log.info("Pago aprobado aplicado: reserva marcada PAGADA (idReserva={}, idTutor={}, idPaseador={}, idTransaccion={}, mp={})",
                saved.getIdReserva(), saved.getIdTutorUsuario(), idPaseador, idTransaccionPagos, mpId);
    }

    /**
     * Best-effort tras COMMIT: generar/persistir comprobante en pagos-service y disparar correo al tutor si está configurado.
     * Idempotente del lado de pagos ({@code UNIQUE(id_reserva)}); reintentos por webhook tardío son seguros.
     */
    private void programarComprobantePostCommit(Integer idReserva) {
        if (idReserva == null || !reservaComprobantePostCommitRunner.isSchedulingEnabled()) {
            return;
        }
        Runnable task = () -> reservaComprobantePostCommitRunner.generarBestEffort(idReserva);
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

    /**
     * Tras COMMIT de la reserva: acreditación billetera (retenido) si corresponde y sincronía agenda (siempre best-effort).
     * El acreditado a {@code EN_RETENIDO} al marcar PAGADA solo si el origen era {@code ACEPTADA}; si era
     * {@code SOLICITADA}/{@code PENDIENTE_PAGO}, se difiere hasta que el paseador acepte ({@code ReservaService}).
     */
    private void programarSincroniaExternaTrasCommitPago(
            Integer idAgendaBloque,
            boolean acreditarBilletera,
            Integer idReservaWallet,
            Long idTransaccionWallet,
            Integer idUsuarioWalker) {
        Runnable task =
                () -> {
                    Integer uid = idUsuarioWalker;
                    // Si no se pudo resolver el id del paseador antes del commit (best-effort),
                    // se reintenta justo después del commit para mejorar la sincronía "instantánea".
                    if (acreditarBilletera
                            && pagosBilleteraIntegracionClient.isEnabled()
                            && uid == null
                            && agendaIntegracionClient.isEnabled()
                            && idAgendaBloque != null
                            && idTransaccionWallet != null
                            && idReservaWallet != null) {
                        try {
                            AgendaBloqueReservaClientDTO bloque =
                                    agendaIntegracionClient.obtenerBloquePorIdInterno(idAgendaBloque);
                            uid = bloque != null ? bloque.getIdUsuario() : null;
                        } catch (RuntimeException e) {
                            log.warn(
                                    "Pago aprobado: reintento idPaseador tras commit falló (idReserva={}, idAgendaBloque={})",
                                    idReservaWallet,
                                    idAgendaBloque,
                                    e);
                        }
                    }

                    if (acreditarBilletera
                            && pagosBilleteraIntegracionClient.isEnabled()
                            && uid != null
                            && idTransaccionWallet != null
                            && idReservaWallet != null) {
                        pagosBilleteraIntegracionClient.acreditarRetenido(
                                idReservaWallet, uid.longValue(), idTransaccionWallet);
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
     * Para mantener la entidad Reserva alineada al MER, ya no persistimos estado transitorio de intentos no aprobados.
     * El estado operativo queda en pagos-service.
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

        log.info(
                "MP no aprobado recibido (idReserva={}, mpPaymentId={}, status={}, detail={}); no se persiste en reserva.",
                idReserva,
                mpPaymentId.trim(),
                mpStatus.trim(),
                truncate(mpStatusDetail, 120));
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
