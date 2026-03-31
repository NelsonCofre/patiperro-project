package com.patiperro.mascota.controller;

import com.patiperro.mascota.model.Tamano;
import com.patiperro.mascota.service.TamanoService;
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
@RequestMapping("/api/mascotas/tamanos")
@RequiredArgsConstructor
public class TamanoController {

    private final TamanoService tamanoService;

    @GetMapping
    public List<Tamano> listar() {
        return tamanoService.listarTodos();
    }

    @PostMapping
    public ResponseEntity<Tamano> crear(@Valid @RequestBody Tamano tamano) {
        return new ResponseEntity<>(tamanoService.crear(tamano), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public Tamano actualizar(@PathVariable Long id, @Valid @RequestBody Tamano body) {
        return tamanoService.actualizar(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tamanoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
