package com.patiperro.tutores.user.controller;

import com.patiperro.tutores.user.dto.FotoRequestDTO;
import com.patiperro.tutores.user.dto.FotoResponseDTO;
import com.patiperro.tutores.user.model.Foto;
import com.patiperro.tutores.user.service.FotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tutores/{tutorId}/fotos")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class FotoController {

    private final FotoService fotoService;

    @GetMapping
    public ResponseEntity<List<FotoResponseDTO>> listarPorTutor(@PathVariable Long tutorId) {
        List<FotoResponseDTO> fotos = fotoService.findByTutorId(tutorId).stream()
                .map(FotoResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(fotos);
    }

    @PostMapping
    public ResponseEntity<FotoResponseDTO> crear(@PathVariable Long tutorId, @RequestBody FotoRequestDTO request) {
        Foto foto = fotoService.crear(tutorId, request.getUrl());
        return ResponseEntity.ok(FotoResponseDTO.fromEntity(foto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        fotoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
