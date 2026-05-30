package com.patiperro.paseador.user.controller;

import com.patiperro.paseador.user.dto.VerificacionIdentidadResponseDTO;
import com.patiperro.paseador.user.service.PaseadorVerificacionService;
import com.patiperro.paseador.user.util.VerificacionDocumentoHttpSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Verificación de identidad del paseador autenticado ({@code /api/paseadores/me/verificacion}).
 * MVP: un único PDF en {@code documento} → aprobación automática.
 */
@RestController
@RequestMapping("/api/paseadores/me/verificacion")
@RequiredArgsConstructor
public class PaseadorVerificacionController {

    private final PaseadorVerificacionService verificacionService;

    @GetMapping
    public VerificacionIdentidadResponseDTO obtenerEstado() {
        return verificacionService.obtenerEstadoAutenticado();
    }

    @PostMapping(value = "/documento", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VerificacionIdentidadResponseDTO> subirDocumento(
            @RequestParam(value = "documento", required = false) MultipartFile documento) {
        requireMultipartPresente(documento, "documento");
        VerificacionIdentidadResponseDTO body = verificacionService.subirDocumento(documento);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/documento")
    public ResponseEntity<Resource> descargarDocumento() throws IOException {
        return buildDocumentoResponse(verificacionService.resolverDocumentoAutenticado());
    }

    @GetMapping("/documentos/{lado}")
    public ResponseEntity<Resource> descargarDocumentoLegacy(@PathVariable String lado) throws IOException {
        return buildDocumentoResponse(verificacionService.resolverDocumentoAutenticado(lado));
    }

    private static ResponseEntity<Resource> buildDocumentoResponse(Path path) throws IOException {
        Resource resource = new UrlResource(path.toUri());
        String contentType = Files.probeContentType(path);
        return VerificacionDocumentoHttpSupport.okDocumento(contentType).body(resource);
    }

    private static void requireMultipartPresente(MultipartFile file, String nombreParametro) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Archivo requerido: " + nombreParametro);
        }
    }
}
