package com.patiperro.agenda.controller;

import com.patiperro.agenda.dto.DiaSemanaRequestDTO;
import com.patiperro.agenda.dto.DiaSemanaResponseDTO;
import com.patiperro.agenda.service.DiaSemanaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agenda/dias-semana")
@RequiredArgsConstructor
public class DiaSemanaController {

    private final DiaSemanaService service;

    @GetMapping
    public List<DiaSemanaResponseDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public DiaSemanaResponseDTO obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<DiaSemanaResponseDTO> crear(@Valid @RequestBody DiaSemanaRequestDTO body) {
        return new ResponseEntity<>(service.crear(body), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public DiaSemanaResponseDTO actualizar(@PathVariable Integer id, @Valid @RequestBody DiaSemanaRequestDTO body) {
        return service.actualizar(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
