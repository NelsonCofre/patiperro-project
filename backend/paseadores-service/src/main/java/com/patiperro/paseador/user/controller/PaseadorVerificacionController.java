package com.patiperro.paseador.user.controller;

import com.patiperro.paseador.user.dto.VerificacionIdentidadResponseDTO;
import com.patiperro.paseador.user.service.PaseadorVerificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
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

@RestController
@RequestMapping("/api/paseadores/me/verificacion")
@RequiredArgsConstructor
public class PaseadorVerificacionController {

    private final PaseadorVerificacionService verificacionService;

    @GetMapping
    public VerificacionIdentidadResponseDTO obtenerEstado() {
        return verificacionService.obtenerEstadoAutenticado();
    }

    @PostMapping(value = "/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VerificacionIdentidadResponseDTO> subirDocumentos(
            @RequestParam("cedulaFrontal") MultipartFile cedulaFrontal,
            @RequestParam("cedulaReverso") MultipartFile cedulaReverso) {
        VerificacionIdentidadResponseDTO body = verificacionService.subirDocumentos(cedulaFrontal, cedulaReverso);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/documentos/{lado}")
    public ResponseEntity<Resource> descargarDocumento(@PathVariable String lado) throws IOException {
        Path path = verificacionService.resolverDocumentoAutenticado(lado);
        Resource resource = new UrlResource(path.toUri());
        String contentType = Files.probeContentType(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE,
                        contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(resource);
    }
}
