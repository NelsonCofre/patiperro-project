package com.patiperro.paseador.controller;

import com.patiperro.paseador.model.EstadoVerificacionIdentidad;
import com.patiperro.paseador.repository.PaseadorRepository;
import com.patiperro.paseador.user.dto.VerificacionIdentidadResponseDTO;
import com.patiperro.paseador.user.service.PaseadorVerificacionService;
import com.patiperro.paseador.user.util.VerificacionDocumentoHttpSupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

/** Lecturas internas para otros microservicios (cabecera secreta, sin JWT). */
@RestController
@RequestMapping("/api/paseadores/interno")
@RequiredArgsConstructor
public class PaseadorInternoController {

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private final PaseadorRepository paseadorRepository;
    private final PaseadorVerificacionService verificacionService;

    @Value("${patiperro.paseadores.interno.secret:}")
    private String internoSecret;

    @GetMapping("/{id}/correo")
    public ResponseEntity<PaseadorCorreoResponse> obtenerCorreo(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @PathVariable Long id) {
        ResponseEntity<Void> forbidden = validarSecreto(secretoHeader);
        if (forbidden != null) {
            return ResponseEntity.status(forbidden.getStatusCode()).build();
        }
        return paseadorRepository
                .findById(id)
                .map(p -> {
                    String c = p.getCorreo();
                    return ResponseEntity.ok(new PaseadorCorreoResponse(c != null ? c.trim() : null));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/verificacion-identidad/documentos/{lado}")
    public ResponseEntity<Resource> descargarDocumentoVerificacion(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @PathVariable Long id,
            @PathVariable String lado) throws IOException {
        ResponseEntity<Void> forbidden = validarSecreto(secretoHeader);
        if (forbidden != null) {
            return ResponseEntity.status(forbidden.getStatusCode()).build();
        }
        Path path = verificacionService.resolverDocumentoPorPaseadorId(id, lado);
        Resource resource = new UrlResource(path.toUri());
        String contentType = Files.probeContentType(path);
        return VerificacionDocumentoHttpSupport.okDocumento(contentType).body(resource);
    }

    @PutMapping("/{id}/verificacion-identidad")
    public ResponseEntity<VerificacionIdentidadResponseDTO> revisarVerificacionIdentidad(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @PathVariable Long id,
            @Valid @RequestBody RevisarVerificacionIdentidadRequest body) {
        ResponseEntity<Void> forbidden = validarSecreto(secretoHeader);
        if (forbidden != null) {
            return ResponseEntity.status(forbidden.getStatusCode()).build();
        }
        VerificacionIdentidadResponseDTO response = verificacionService.revisarVerificacionInterna(
                id,
                body.estado(),
                body.motivo());
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Void> validarSecreto(String secretoHeader) {
        if (!StringUtils.hasText(internoSecret)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!secretMatches(internoSecret, secretoHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return null;
    }

    private static boolean secretMatches(String expected, String provided) {
        if (expected == null || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }

    public record PaseadorCorreoResponse(String correo) {}

    public record RevisarVerificacionIdentidadRequest(
            @NotNull EstadoVerificacionIdentidad estado,
            String motivo) {}
}
