package com.patiperro.tutores.user.controller;

import com.patiperro.tutores.user.service.TutorService;
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
@RequestMapping("/api/tutores/interno")
@RequiredArgsConstructor
public class TutorInternoController {

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private final TutorService tutorService;

    @Value("${patiperro.tutores.interno.secret:}")
    private String internoSecret;

    @GetMapping("/{id}/correo")
    public ResponseEntity<TutorCorreoResponse> obtenerCorreo(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @PathVariable Long id) {
        if (!StringUtils.hasText(internoSecret)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!internoSecret.equals(secretoHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            var tutor = tutorService.findById(id);
            String c = tutor.getCorreo();
            return ResponseEntity.ok(new TutorCorreoResponse(c != null ? c.trim() : null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    public record TutorCorreoResponse(String correo) {
    }
}
