package com.patiperro.tutores.user.controller;

import com.patiperro.tutores.user.dto.TutorResponseDTO;
import com.patiperro.tutores.user.service.TutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Consulta de perfil del dueño de la mascota (entidad {@code Tutor}). */
@RestController
@RequestMapping("/api/tutores")
@RequiredArgsConstructor
public class TutorController {

    private final TutorService tutorService;

    @GetMapping("/{id}")
    public ResponseEntity<TutorResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(TutorResponseDTO.fromEntity(tutorService.findById(id)));
    }
}
