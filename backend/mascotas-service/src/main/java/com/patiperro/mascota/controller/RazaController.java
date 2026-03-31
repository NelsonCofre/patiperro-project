package com.patiperro.mascota.controller;

import com.patiperro.mascota.model.Raza;
import com.patiperro.mascota.service.RazaService;
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
@RequestMapping("/api/mascotas/razas")
@RequiredArgsConstructor
public class RazaController {

    private final RazaService razaService;

    @GetMapping
    public List<Raza> obtenerTodas() {
        return razaService.listarTodas();
    }

    @PostMapping
    public ResponseEntity<Raza> crear(@Valid @RequestBody Raza raza) {
        return new ResponseEntity<>(razaService.crear(raza), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public Raza actualizar(@PathVariable Long id, @Valid @RequestBody Raza body) {
        return razaService.actualizar(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        razaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
