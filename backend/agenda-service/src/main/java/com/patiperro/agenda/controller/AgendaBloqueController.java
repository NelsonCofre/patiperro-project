package com.patiperro.agenda.controller;

import com.patiperro.agenda.dto.AgendaBloqueRequestDTO;
import com.patiperro.agenda.dto.AgendaBloqueResponseDTO;
import com.patiperro.agenda.dto.AgendaBloqueSerieMensualRequestDTO;
import com.patiperro.agenda.dto.AgendaBloqueSerieMensualResponseDTO;
import com.patiperro.agenda.service.AgendaBloqueService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/agenda/bloques")
@RequiredArgsConstructor
public class AgendaBloqueController {

    private final AgendaBloqueService service;

    @GetMapping
    public List<AgendaBloqueResponseDTO> listar() {
        return service.listar();
    }

    @GetMapping("/usuario/{idUsuario}")
    public List<AgendaBloqueResponseDTO> listarPorUsuario(@PathVariable Integer idUsuario) {
        return service.listarPorUsuario(idUsuario);
    }

    /**
     * Bloques ofertables en rango: se omiten días marcados como bloqueo personal
     * ({@code /api/agenda/bloqueos-dia}).
     */
    @GetMapping("/usuario/{idUsuario}/oferta")
    public List<AgendaBloqueResponseDTO> listarBloquesOfertables(
            @PathVariable Integer idUsuario,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.listarBloquesOfertables(idUsuario, desde, hasta);
    }

    @GetMapping("/{id}")
    public AgendaBloqueResponseDTO obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<AgendaBloqueResponseDTO> crear(@Valid @RequestBody AgendaBloqueRequestDTO body) {
        return new ResponseEntity<>(service.crear(body), HttpStatus.CREATED);
    }

    /**
     * Misma franja horaria en todas las ocurrencias del día de la semana de {@code fechaSemilla}
     * dentro de ese mes; omite fechas pasadas y solapes.
     */
    @PostMapping("/serie-mes")
    public ResponseEntity<AgendaBloqueSerieMensualResponseDTO> crearSerieMensual(
            @Valid @RequestBody AgendaBloqueSerieMensualRequestDTO body) {
        return new ResponseEntity<>(service.crearSerieMensualEnMes(body), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public AgendaBloqueResponseDTO actualizar(@PathVariable Integer id, @Valid @RequestBody AgendaBloqueRequestDTO body) {
        return service.actualizar(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
