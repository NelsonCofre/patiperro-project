package com.patiperro.reserva.service;

import com.patiperro.reserva.dto.EncuentroConfirmadoEventDTO;
import com.patiperro.reserva.dto.MascotaInternoDetalleResponseDTO;
import com.patiperro.reserva.dto.integracion.AgendaBloqueReservaClientDTO;
import com.patiperro.reserva.dto.integracion.TutorReservaClientDTO;
import com.patiperro.reserva.event.PaseoIniciadoEvent;
import com.patiperro.reserva.model.Reserva;
import com.patiperro.reserva.repository.ReservaRepository;
import com.patiperro.reserva.support.AgendaIntegracionClient;
import com.patiperro.reserva.support.MascotaIntegracionClient;
import com.patiperro.reserva.support.TutorIntegracionClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Efectos colaterales tras iniciar el paseo (STOMP + integraciones opcionales).
 * Se ejecuta preferentemente {@code AFTER_COMMIT} vía {@link com.patiperro.reserva.event.PaseoIniciadoEventListener}.
 */
@Service
@RequiredArgsConstructor
public class PaseoInicioSideEffectsService {

    private static final Logger log = LoggerFactory.getLogger(PaseoInicioSideEffectsService.class);

    private final ReservaRepository reservaRepository;
    private final AgendaIntegracionClient agendaIntegracionClient;
    private final MascotaIntegracionClient mascotaIntegracionClient;
    private final TutorIntegracionClient tutorIntegracionClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestClient.Builder restClientBuilder;

    @Value("${patiperro.reserva.integracion.tracking.enabled:false}")
    private boolean trackingIntegracionEnabled;

    @Value("${patiperro.reserva.integracion.chat.enabled:false}")
    private boolean chatIntegracionEnabled;

    @Value("${patiperro.reserva.integracion.tracking.base-url:}")
    private String trackingBaseUrl;

    @Value("${patiperro.reserva.integracion.chat.base-url:}")
    private String chatBaseUrl;

    public void ejecutar(PaseoIniciadoEvent event) {
        if (event == null || event.getIdReserva() == null) {
            return;
        }
        Reserva reserva = reservaRepository.findById(event.getIdReserva()).orElse(null);
        if (reserva == null) {
            log.warn("PaseoIniciado: reserva {} no encontrada para efectos secundarios", event.getIdReserva());
            return;
        }
        notificarEncuentroConfirmado(reserva, event.getRawJwtPaseador());
        invocarTrackingSiConfigurado(reserva.getIdReserva());
        invocarChatSiConfigurado(reserva.getIdReserva());
    }

    private void notificarEncuentroConfirmado(Reserva reserva, String rawJwt) {
        try {
            AgendaBloqueReservaClientDTO bloque =
                    agendaIntegracionClient.obtenerBloquePorId(reserva.getIdAgendaBloque(), rawJwt);
            TutorReservaClientDTO tutor =
                    tutorIntegracionClient.obtenerTutor(reserva.getIdTutorUsuario().longValue(), rawJwt);
            MascotaInternoDetalleResponseDTO mascota =
                    mascotaIntegracionClient.obtenerDetalleInterno(reserva.getIdMascota());

            Integer idPaseador = bloque != null ? bloque.getIdUsuario() : null;
            String mascotaNombre = mascota != null && mascota.getNombre() != null && !mascota.getNombre().isBlank()
                    ? mascota.getNombre()
                    : "Mascota #" + reserva.getIdMascota();
            String direccionInicio = tutor != null && tutor.getDireccion() != null
                    ? lineaDireccionTutor(tutor.getDireccion())
                    : "";
            EncuentroConfirmadoEventDTO eventDto = new EncuentroConfirmadoEventDTO(
                    "ENCUENTRO_CONFIRMADO",
                    reserva.getIdReserva(),
                    reserva.getIdTutorUsuario(),
                    idPaseador,
                    "¡El paseo ha comenzado! Tu mascota está en buenas manos",
                    "Paseo iniciado correctamente",
                    mascotaNombre,
                    direccionInicio,
                    reserva.getFechaInicioReal(),
                    true,
                    true
            );

            messagingTemplate.convertAndSend("/topic/reservas/" + reserva.getIdReserva() + "/encuentro", eventDto);
            messagingTemplate.convertAndSend("/topic/tutor/" + reserva.getIdTutorUsuario() + "/encuentro", eventDto);
            if (idPaseador != null) {
                messagingTemplate.convertAndSend("/topic/paseador/" + idPaseador + "/encuentro", eventDto);
            }
        } catch (RuntimeException e) {
            log.warn("Fallo en notificación STOMP post-inicio, idReserva={}", reserva.getIdReserva(), e);
        }
    }

    private void invocarTrackingSiConfigurado(Integer idReserva) {
        if (!trackingIntegracionEnabled) {
            return;
        }
        if (trackingBaseUrl == null || trackingBaseUrl.isBlank()) {
            log.info("Tracking integración habilitada pero sin base-url; no se invoca (reserva={})", idReserva);
            return;
        }
        try {
            String json = "{\"idReserva\":" + idReserva + "}";
            restClientBuilder.build()
                    .post()
                    .uri(trackingBaseUrl + "/internal/tracking/sesiones")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            log.warn("Tracking: llamada no completada para reserva {}", idReserva, e);
        }
    }

    private void invocarChatSiConfigurado(Integer idReserva) {
        if (!chatIntegracionEnabled) {
            return;
        }
        if (chatBaseUrl == null || chatBaseUrl.isBlank()) {
            log.info("Chat integración habilitada pero sin base-url; no se invoca (reserva={})", idReserva);
            return;
        }
        try {
            String json = "{\"idReserva\":" + idReserva + "}";
            restClientBuilder.build()
                    .post()
                    .uri(chatBaseUrl + "/internal/chat/sesiones")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            log.warn("Chat: llamada no completada para reserva {}", idReserva, e);
        }
    }

    private static String lineaDireccionTutor(TutorReservaClientDTO.DireccionTutorClientDTO d) {
        List<String> partes = new ArrayList<>();
        if (d.getCalle() != null && !d.getCalle().isBlank()) {
            partes.add(d.getCalle().trim());
        }
        if (d.getNumeracion() != null) {
            partes.add(String.valueOf(d.getNumeracion()));
        }
        if (d.getCasaDepartamento() != null && !d.getCasaDepartamento().isBlank()) {
            partes.add(d.getCasaDepartamento().trim());
        }
        if (d.getCiudad() != null && !d.getCiudad().isBlank()) {
            partes.add(d.getCiudad().trim());
        }
        if (d.getComuna() != null && !d.getComuna().isBlank()) {
            partes.add(d.getComuna().trim());
        }
        return String.join(", ", partes);
    }
}
