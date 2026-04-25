package com.patiperro.notification_service.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.patiperro.notification_service.service.PaseoInicioNotificacionService;

import java.util.Map;

/**
 * Endpoints servidor-a-servidor ligados al ciclo de paseo (placeholder).
 * Las rutas mutables se protegen con cabecera {@link #HEADER_INTERNO}, no con JWT de usuario.
 */
@RestController
@RequestMapping("/internal/paseo")
public class InternalPaseoController {

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private final PaseoInicioNotificacionService paseoInicioNotificacionService;

    @Value("${patiperro.notification.interno.secret:}")
    private String internoSecret;

    public InternalPaseoController(PaseoInicioNotificacionService paseoInicioNotificacionService) {
        this.paseoInicioNotificacionService = paseoInicioNotificacionService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "modulo", "paseo-interno");
    }

    /**
     * Recepción de evento de inicio de paseo (p. ej. invocado por otro microservicio en el futuro).
     * Cuerpo JSON: campo {@code idReserva} obligatorio.
     */
    @PostMapping("/inicio")
    public ResponseEntity<Void> notificarInicioPaseo(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @RequestBody(required = false) PaseoInicioInternoRequest body) {
        if (!StringUtils.hasText(internoSecret)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!internoSecret.equals(secretoHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (body == null || body.idReserva() == null) {
            return ResponseEntity.badRequest().build();
        }
        paseoInicioNotificacionService.procesarNotificacionInicioPaseo(body.idReserva());
        return ResponseEntity.noContent().build();
    }

    public record PaseoInicioInternoRequest(Integer idReserva) {}
}
