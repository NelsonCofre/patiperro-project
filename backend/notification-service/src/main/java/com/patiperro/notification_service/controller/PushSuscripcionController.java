package com.patiperro.notification_service.controller;

import com.patiperro.notification_service.dto.PushSuscripcionRequest;
import com.patiperro.notification_service.dto.PushSuscripcionResponse;
import com.patiperro.notification_service.dto.VapidPublicKeyResponse;
import com.patiperro.notification_service.service.PushSuscripcionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API pública de suscripciones Web Push del chat ({@code /api/notificaciones/push/**}).
 * Requiere JWT (tutorId o paseadorId); el {@code idUsuario} no se envía en el body.
 */
@RestController
@RequestMapping("/api/notificaciones/push")
@RequiredArgsConstructor
public class PushSuscripcionController {

    private final PushSuscripcionService pushSuscripcionService;

    /** Clave VAPID pública para {@code pushManager.subscribe()} en el navegador. */
    @GetMapping("/vapid-public-key")
    public VapidPublicKeyResponse vapidPublicKey() {
        return pushSuscripcionService.obtenerClavePublicaVapid();
    }

    /** Registra o actualiza la suscripción del dispositivo actual (UPSERT por {@code endpoint}). */
    @PostMapping("/suscripciones")
    public ResponseEntity<PushSuscripcionResponse> registrar(
            @AuthenticationPrincipal Integer idUsuario,
            @Valid @RequestBody PushSuscripcionRequest body) {
        if (idUsuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pushSuscripcionService.registrar(idUsuario, body));
    }

    /** Elimina la suscripción del usuario autenticado para el {@code endpoint} indicado. */
    @DeleteMapping("/suscripciones")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal Integer idUsuario,
            @RequestParam String endpoint) {
        if (idUsuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        pushSuscripcionService.eliminar(idUsuario, endpoint.trim());
        return ResponseEntity.noContent().build();
    }
}
