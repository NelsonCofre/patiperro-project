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
     * Idempotente: si ya está PAGADA, no realiza cambios.
     */
    @Transactional
    public void marcarReservaComoPagada(Integer idReserva, String mpPaymentId) {
        if (idReserva == null) {
            throw new IllegalArgumentException("idReserva es obligatorio");
        }
        Reserva r = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        // 1) Idempotencia: si ya está pagada, no repetir efectos.
        EstadoReserva actual = r.getEstadoReserva();
        String mpId = safe(mpPaymentId);
        if (actual != null) {
            Integer idEstado = actual.getIdEstadoReserva();
            String nombre = actual.getNombreEstado();
            boolean yaPagada = (idEstado != null && idEstado.equals(EstadoReservaCatalogo.ID_PAGADA))
                    || (nombre != null && nombre.trim().equalsIgnoreCase(EstadoReservaCatalogo.NOMBRE_PAGADA));
            if (yaPagada) {
                log.info("Pago aprobado idempotente: reserva ya estaba PAGADA (idReserva={}, mpPaymentId={})",
                        idReserva, mpId);
                if (mpId != null && !mpId.equals(safe(r.getMercadopagoPaymentId()))) {
                    if (!StringUtils.hasText(r.getMercadopagoPaymentId())) {
                        r.setMercadopagoPaymentId(mpId);
                        reservaRepository.save(r);
                    } else {
                        log.warn("Pago aprobado idempotente: reserva ya tenía otro mercadopago_payment_id; no se sobrescribe (idReserva={})",
                                idReserva);
                    }
                }
                return;
            }
        }

        // 2) Validación + actualización atómica (solo desde estados permitidos)
        EstadoReserva pagada = estadoReservaService.obtenerPorNombreIgnoreCase(EstadoReservaCatalogo.NOMBRE_PAGADA);
        List<Integer> estadosOrigenPermitidos = List.of(
                EstadoReservaCatalogo.ID_SOLICITADA,
                EstadoReservaCatalogo.ID_ACEPTADA,
                EstadoReservaCatalogo.ID_PENDIENTE_PAGO
        );
        int updated = reservaRepository.marcarPagadaSiEstadoEn(pagada, idReserva, estadosOrigenPermitidos, mpId);
        if (updated != 1) {
            // Releer para explicar el conflicto con el estado real actual.
            Reserva actualizada = reservaRepository.findById(idReserva)
                    .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
            String estado = actualizada.getEstadoReserva() != null ? actualizada.getEstadoReserva().getNombreEstado() : "SIN_ESTADO";
            throw new IllegalStateException("No se puede marcar PAGADA desde estado actual: " + estado);
        }

        Reserva saved = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        // 3) Notificación inmediata al paseador (y canales auxiliares) vía STOMP
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
                safe(mpPaymentId)
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

        log.info("Pago aprobado aplicado: reserva marcada PAGADA (idReserva={}, idTutor={}, idPaseador={}, mpPaymentId={})",
                saved.getIdReserva(), saved.getIdTutorUsuario(), idPaseador, safe(mpPaymentId));
    }

    public record PagoConfirmadoEventDTO(
            String type,
            Integer idReserva,
            Integer idTutorUsuario,
            Integer idPaseadorUsuario,
            String mpPaymentId
    ) {
    }

    private static String safe(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
