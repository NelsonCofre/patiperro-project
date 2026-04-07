package com.patiperro.reserva.controller;

import com.patiperro.reserva.dto.EstadoReservaRequestDTO;
import com.patiperro.reserva.dto.EstadoReservaResponseDTO;
import com.patiperro.reserva.service.EstadoReservaService;
import jakarta.validation.Valid;
import java.util.List;
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

@RestController
@RequestMapping("/api/reserva/estados")
@RequiredArgsConstructor
public class EstadoReservaController {

    private final EstadoReservaService service;

    @GetMapping
    public List<EstadoReservaResponseDTO> listar() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public EstadoReservaResponseDTO obtener(@PathVariable Integer id) {
        return service.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<EstadoReservaResponseDTO> crear(@Valid @RequestBody EstadoReservaRequestDTO body) {
        return new ResponseEntity<>(service.crear(body), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public EstadoReservaResponseDTO actualizar(
            @PathVariable Integer id, @Valid @RequestBody EstadoReservaRequestDTO body) {
        return service.actualizar(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
