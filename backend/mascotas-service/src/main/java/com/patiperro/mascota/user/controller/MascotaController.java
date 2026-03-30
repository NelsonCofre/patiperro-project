package com.patiperro.mascota.user.controller;

import com.patiperro.mascota.model.Mascota;
import com.patiperro.mascota.user.service.MascotaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull; // Importante: agrega este import
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/mascotas")
@CrossOrigin(origins = "*")
public class MascotaController {

    @Autowired
    private MascotaService mascotaService;

    @PostMapping
    public ResponseEntity<Mascota> crearMascota(@Valid @RequestBody @NonNull Mascota mascota) {
        // Al agregar @NonNull arriba, esta advertencia debería desaparecer
        Mascota nuevaMascota = mascotaService.registrarMascota(mascota);
        return new ResponseEntity<>(nuevaMascota, HttpStatus.CREATED);
    }

    @GetMapping("/tutor/{idTutor}")
    public List<Mascota> obtenerPorTutor(@PathVariable Long idTutor) {
        return mascotaService.listarPorTutor(idTutor);
    }
}