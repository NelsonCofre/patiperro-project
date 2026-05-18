package com.patiperro.notification_service.service;

import com.patiperro.notification_service.config.WebPushProperties;
import com.patiperro.notification_service.dto.PushSuscripcionRequest;
import com.patiperro.notification_service.dto.PushSuscripcionResponse;
import com.patiperro.notification_service.dto.VapidPublicKeyResponse;
import com.patiperro.notification_service.model.PushSuscripcion;
import com.patiperro.notification_service.repository.PushSuscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Objects;

/**
 * Suscripciones Web Push del chat ({@code push_suscripcion}).
 * El {@code idUsuario} siempre proviene del JWT (tutorId o paseadorId), nunca del body.
 */
@Service
@RequiredArgsConstructor
public class PushSuscripcionService {

    private static final int USER_AGENT_MAX_LENGTH = 512;

    private final PushSuscripcionRepository pushSuscripcionRepository;
    private final WebPushProperties webPushProperties;

    @Transactional(readOnly = true)
    public VapidPublicKeyResponse obtenerClavePublicaVapid() {
        if (!webPushProperties.hasPublicKey()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Clave VAPID pública no configurada");
        }
        return new VapidPublicKeyResponse(webPushProperties.getVapid().getPublicKey());
    }

    /**
     * Registra o actualiza la suscripción del dispositivo (UPSERT por {@code endpoint}).
     */
    @Transactional
    public PushSuscripcionResponse registrar(Integer idUsuarioJwt, PushSuscripcionRequest req) {
        validarIdUsuario(idUsuarioJwt);
        validarVapidPublicaParaSuscripcion();

        String endpoint = req.endpoint().trim();
        PushSuscripcion existente = pushSuscripcionRepository.findByEndpoint(endpoint).orElse(null);

        if (existente != null && !Objects.equals(existente.getIdUsuario(), idUsuarioJwt)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La suscripción ya pertenece a otro usuario");
        }

        PushSuscripcion suscripcion = existente != null ? existente : new PushSuscripcion();
        suscripcion.setIdUsuario(idUsuarioJwt);
        suscripcion.setEndpoint(endpoint);
        suscripcion.setP256dhKey(req.p256dh().trim());
        suscripcion.setAuthKey(req.auth().trim());
        aplicarUserAgent(suscripcion, req.userAgent());
        suscripcion.setActiva(true);
        suscripcion.setFechaUltimoUso(Instant.now());
        if (suscripcion.getFechaAlta() == null) {
            suscripcion.setFechaAlta(Instant.now());
        }

        try {
            suscripcion = pushSuscripcionRepository.save(suscripcion);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La suscripción ya está registrada para este dispositivo");
        }
        return toResponse(suscripcion);
    }

    @Transactional
    public void eliminar(Integer idUsuarioJwt, String endpoint) {
        validarIdUsuario(idUsuarioJwt);
        if (endpoint == null || endpoint.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endpoint es obligatorio");
        }

        String endpointNormalizado = endpoint.trim();
        PushSuscripcion suscripcion = pushSuscripcionRepository.findByEndpoint(endpointNormalizado)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Suscripción no encontrada"));

        if (!Objects.equals(suscripcion.getIdUsuario(), idUsuarioJwt)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puede eliminar esta suscripción");
        }

        pushSuscripcionRepository.delete(suscripcion);
    }

    private void validarVapidPublicaParaSuscripcion() {
        if (!webPushProperties.hasPublicKey()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Web Push no disponible: configure la clave VAPID pública");
        }
    }

    private static void validarIdUsuario(Integer idUsuarioJwt) {
        if (idUsuarioJwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        if (idUsuarioJwt <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idUsuario inválido");
        }
    }

    private static void aplicarUserAgent(PushSuscripcion suscripcion, String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return;
        }
        String ua = userAgent.trim();
        suscripcion.setUserAgent(
                ua.length() > USER_AGENT_MAX_LENGTH ? ua.substring(0, USER_AGENT_MAX_LENGTH) : ua);
    }

    private static PushSuscripcionResponse toResponse(PushSuscripcion s) {
        return new PushSuscripcionResponse(
                s.getIdSuscripcion(),
                s.getIdUsuario(),
                s.getEndpoint(),
                s.getFechaAlta());
    }
}
