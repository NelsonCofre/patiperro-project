package com.patiperro.agenda.controller;

import com.patiperro.agenda.dto.EstadoBloqueRequestDTO;
import com.patiperro.agenda.dto.EstadoBloqueResponseDTO;
import com.patiperro.agenda.service.EstadoBloqueService;
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
@RequestMapping("/api/agenda/estados-bloque")
@RequiredArgsConstructor
public class EstadoBloqueController {

    private final EstadoBloqueService service;

    @GetMapping
    public List<EstadoBloqueResponseDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public EstadoBloqueResponseDTO obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<EstadoBloqueResponseDTO> crear(@Valid @RequestBody EstadoBloqueRequestDTO body) {
        return new ResponseEntity<>(service.crear(body), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public EstadoBloqueResponseDTO actualizar(@PathVariable Integer id, @Valid @RequestBody EstadoBloqueRequestDTO body) {
        return service.actualizar(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
