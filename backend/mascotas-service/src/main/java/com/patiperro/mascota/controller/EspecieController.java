package com.patiperro.mascota.controller;

import com.patiperro.mascota.model.Especie;
import com.patiperro.mascota.service.EspecieService;
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
@RequestMapping("/api/mascotas/especies")
@RequiredArgsConstructor
public class EspecieController {

    private final EspecieService especieService;

    @GetMapping
    public List<Especie> listar() {
        return especieService.listarTodas();
    }

    @PostMapping
    public ResponseEntity<Especie> crear(@Valid @RequestBody Especie especie) {
        return new ResponseEntity<>(especieService.crear(especie), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public Especie actualizar(@PathVariable Long id, @Valid @RequestBody Especie body) {
        return especieService.actualizar(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        especieService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
