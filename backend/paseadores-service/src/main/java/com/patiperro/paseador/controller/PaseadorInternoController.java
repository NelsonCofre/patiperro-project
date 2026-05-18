package com.patiperro.paseador.controller;

import com.patiperro.paseador.repository.PaseadorRepository;
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

/** Lecturas internas para otros microservicios (cabecera secreta, sin JWT). */
@RestController
@RequestMapping("/api/paseadores/interno")
@RequiredArgsConstructor
public class PaseadorInternoController {

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private final PaseadorRepository paseadorRepository;

    @Value("${patiperro.paseadores.interno.secret:}")
    private String internoSecret;

    @GetMapping("/{id}/correo")
    public ResponseEntity<PaseadorCorreoResponse> obtenerCorreo(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @PathVariable Long id) {
        if (!StringUtils.hasText(internoSecret)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!internoSecret.equals(secretoHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return paseadorRepository
                .findById(id)
                .map(p -> {
                    String c = p.getCorreo();
                    return ResponseEntity.ok(new PaseadorCorreoResponse(c != null ? c.trim() : null));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record PaseadorCorreoResponse(String correo) {}
}
