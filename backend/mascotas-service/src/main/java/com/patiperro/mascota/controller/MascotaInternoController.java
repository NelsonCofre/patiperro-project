package com.patiperro.mascota.controller;

import com.patiperro.mascota.dto.PortadaIntegracion;
import com.patiperro.mascota.service.MascotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints solo para otros microservicios (p. ej. reserva-service). Protegidos por secreto compartido,
 * no por JWT de usuario.
 */
@RestController
@RequestMapping("/api/mascotas/interno")
@RequiredArgsConstructor
public class MascotaInternoController {

    static final String HEADER_SECRETO = "X-Patiperro-Interno-Secret";

    private final MascotaService mascotaService;

    @Value("${patiperro.mascotas.interno.secret:}")
    private String internoSecret;

    /**
     * URL de portada: {@code foto_perfil} o primera foto de la galería.
     */
    @GetMapping("/{idMascota}/portada-url")
    public ResponseEntity<Map<String, String>> portadaUrl(
            @PathVariable Long idMascota,
            @RequestHeader(value = HEADER_SECRETO, required = false) String secretoHeader) {
        if (!StringUtils.hasText(internoSecret)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!internoSecret.equals(secretoHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        PortadaIntegracion datos = mascotaService.obtenerPortadaParaIntegracion(idMascota);
        if (datos == null) {
            return ResponseEntity.notFound().build();
        }
        if (!StringUtils.hasText(datos.url())) {
            return ResponseEntity.notFound().build();
        }
        String nombre = StringUtils.hasText(datos.nombre()) ? datos.nombre() : "";
        return ResponseEntity.ok(Map.of("url", datos.url().trim(), "nombre", nombre));
    }
}
