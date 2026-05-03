package com.patiperro.reserva.service;

import com.patiperro.reserva.dto.PaseadorResumenDTO;
import com.patiperro.reserva.dto.integracion.AgendaBloqueReservaClientDTO;
import com.patiperro.reserva.model.EstadoReserva;
import com.patiperro.reserva.model.EstadoReservaCatalogo;
import com.patiperro.reserva.model.Reserva;
import com.patiperro.reserva.repository.ReservaRepository;
import com.patiperro.reserva.support.AgendaIntegracionClient;
import com.patiperro.reserva.support.NotificacionPagoIntegracionClient;
import com.patiperro.reserva.support.PaseadorIntegracionClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Lógica de negocio para aplicar efectos de pago en la reserva.
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
     * Idempotente: si ya está PAGADA, no repite efectos laterales.
     *
     * @param idTransaccionPagos {@code transaccion.id_transaccion} en pagos-service
     * @param mpPaymentId        opcional; solo para eventos STOMP / logs (el cobro MP queda en pagos)
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
                if (r.getIdPago() == null) {
                    r.setIdPago(idTransaccionPagos);
                    reservaRepository.save(r);
                } else if (!r.getIdPago().equals(idTransaccionPagos)) {
                    log.warn("Pago aprobado idempotente: reserva ya tenía otro id_pago; no se sobrescribe (idReserva={})",
                            idReserva);
                }
                return;
            }
        }

        EstadoReserva pagada = estadoReservaService.obtenerPorNombreIgnoreCase(EstadoReservaCatalogo.NOMBRE_PAGADA);
        List<Integer> estadosOrigenPermitidos = List.of(
                EstadoReservaCatalogo.ID_SOLICITADA,
                EstadoReservaCatalogo.ID_ACEPTADA,
                EstadoReservaCatalogo.ID_PENDIENTE_PAGO
        );
        Reserva rUpdate = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
        EstadoReserva estadoActualUpd = rUpdate.getEstadoReserva();
        Integer idEstadoUpd = estadoActualUpd != null ? estadoActualUpd.getIdEstadoReserva() : null;
        if (idEstadoUpd == null || !estadosOrigenPermitidos.contains(idEstadoUpd)) {
            String estado = estadoActualUpd != null ? estadoActualUpd.getNombreEstado() : "SIN_ESTADO";
            throw new IllegalStateException("No se puede marcar PAGADA desde estado actual: " + estado);
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

        log.info("Pago aprobado aplicado: reserva marcada PAGADA (idReserva={}, idTutor={}, idPaseador={}, idTransaccion={}, mp={})",
                saved.getIdReserva(), saved.getIdTutorUsuario(), idPaseador, idTransaccionPagos, mpId);
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
