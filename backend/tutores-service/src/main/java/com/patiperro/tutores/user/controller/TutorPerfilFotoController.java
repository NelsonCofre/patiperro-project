package com.patiperro.tutores.user.controller;

import com.patiperro.tutores.user.service.TutorPerfilFotoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TutorPerfilFotoController {

    private final TutorPerfilFotoStorageService storageService;

    /**
     * Ruta relativa (desde el gateway) para guardar en tutor.foto_perfil; el front antepone API_BASE_URL al mostrar.
     */
    @PostMapping(value = "/api/auth/tutores/upload-foto-perfil", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            String filename = storageService.save(file);
            String url = "/api/tutores/public/perfil/" + filename;
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().body(Map.of("message", "No se pudo guardar la imagen"));
        }
    }

    @GetMapping("/api/tutores/public/perfil/{filename:.+}")
    public ResponseEntity<Resource> serve(@PathVariable String filename) throws IOException {
        Path path = storageService.resolveExisting(filename);
        if (path == null) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new UrlResource(path.toUri());
        String contentType = Files.probeContentType(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .body(resource);
    }
}
