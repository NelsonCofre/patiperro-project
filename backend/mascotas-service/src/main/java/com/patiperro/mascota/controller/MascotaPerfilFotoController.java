package com.patiperro.mascota.controller;

import com.patiperro.mascota.model.Mascota;
import com.patiperro.mascota.security.TutorSecurity;
import com.patiperro.mascota.service.MascotaFotoStorageService;
import com.patiperro.mascota.service.MascotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Subida y entrega pública de foto de perfil de mascota (misma convención que
 * TutorPerfilFotoController / PaseadorPerfilFotoController).
 */
@RestController
@RequiredArgsConstructor
public class MascotaPerfilFotoController {

    private final MascotaService mascotaService;
    private final MascotaFotoStorageService mascotaFotoStorageService;

    @PatchMapping(value = "/api/mascotas/{id}/foto-perfil", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> actualizarFotoPerfilPatch(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return actualizarFotoPerfil(id, file);
    }

    /** Alias POST para clientes que no envían PATCH con multipart. */
    @PostMapping(value = "/api/mascotas/{id}/foto-perfil", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> actualizarFotoPerfilPost(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return actualizarFotoPerfil(id, file);
    }

    @GetMapping("/api/mascotas/public/mascota/{filename:.+}")
    public ResponseEntity<Resource> servirFoto(@PathVariable String filename) throws IOException {
        Path path = mascotaFotoStorageService.resolveExisting(filename);
        if (path == null) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new UrlResource(path.toUri());
        String contentType = Files.probeContentType(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE,
                        contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .body(resource);
    }

    private ResponseEntity<?> actualizarFotoPerfil(Long id, MultipartFile file) {
        long idTutor = TutorSecurity.requireTutor().tutorId();
        try {
            Mascota actualizada = mascotaService.actualizarFotoPerfil(id, file, idTutor);
            return ResponseEntity.ok(Map.of(
                    "idMascota", actualizada.getIdMascota(),
                    "fotoPerfil", actualizada.getFotoPerfil(),
                    "message", "Foto de mascota actualizada"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "No se pudo guardar la imagen"));
        }
    }
}
