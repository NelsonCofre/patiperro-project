package com.patiperro.tutores.user.controller;

import com.patiperro.tutores.user.dto.CambiarContrasenaRequestDTO;
import com.patiperro.tutores.user.dto.MiPerfilResponseDTO;
import com.patiperro.tutores.user.service.TutorPerfilService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/tutores/me")
@RequiredArgsConstructor
public class TutorPerfilController {

    private final TutorPerfilService tutorPerfilService;

    @GetMapping("/perfil")
    public ResponseEntity<MiPerfilResponseDTO> getMyProfile() {
        return ResponseEntity.ok(tutorPerfilService.getMyProfile());
    }

    @PatchMapping(value = "/foto-perfil", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateFotoPerfil(@RequestParam("file") MultipartFile file) {
        try {
            MiPerfilResponseDTO perfil = tutorPerfilService.updateFotoPerfil(file);
            return ResponseEntity.ok(Map.of(
                    "perfil", perfil,
                    "fotoPerfil", perfil.getFotoPerfil(),
                    "message", "Foto de perfil actualizada"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "No se pudo guardar la imagen"));
        }
    }

    @PatchMapping("/contrasena")
    public ResponseEntity<?> changePassword(@Valid @RequestBody CambiarContrasenaRequestDTO request) {
        try {
            tutorPerfilService.changePassword(request);
            return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
