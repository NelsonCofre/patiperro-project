package com.patiperro.reserva.controller;

import com.patiperro.reserva.dto.ReservaRequestDTO;
import com.patiperro.reserva.dto.ReservaResponseDTO;
import com.patiperro.reserva.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reserva")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService service;

    // =========================================================================
    // ENDPOINT DE VIGILANCIA INTERNA: Conflicto de Bloqueo
    // =========================================================================
    /**
     * Verifica si una lista de bloques de agenda tiene compromisos activos.
     * Es utilizado por el microservicio de Agenda antes de permitir ediciones.
     * URL: POST http://localhost:8085/api/reserva/interno/conflicto-bloqueo
     */
    @PostMapping("/interno/conflicto-bloqueo")
    public boolean conflictoPorBloques(@RequestBody List<Integer> idsAgendaBloque) {
        return service.tieneReservasComprometidasEnBloques(idsAgendaBloque);
    }

    // =========================================================================
    // CRUD Y LISTADOS EXISTENTES
    // =========================================================================

    @GetMapping("/tutor/{idTutorUsuario}")
    public List<ReservaResponseDTO> listarPorTutor(@PathVariable Integer idTutorUsuario) {
        return service.listarPorTutor(idTutorUsuario);
    }

    @GetMapping("/mascota/{idMascota}")
    public List<ReservaResponseDTO> listarPorMascota(@PathVariable Integer idMascota) {
        return service.listarPorMascota(idMascota);
    }

    @GetMapping("/agenda/{idAgendaBloque}")
    public List<ReservaResponseDTO> listarPorAgenda(@PathVariable Integer idAgendaBloque) {
        return service.listarPorAgenda(idAgendaBloque);
    }

    @GetMapping("/estado/{idEstadoReserva}")
    public List<ReservaResponseDTO> listarPorEstado(@PathVariable Integer idEstadoReserva) {
        return service.listarPorEstado(idEstadoReserva);
    }

    @GetMapping
    public List<ReservaResponseDTO> listar() {
        return service.listarTodas();
    }

    @GetMapping("/{id}")
    public ReservaResponseDTO obtener(@PathVariable Integer id) {
        return service.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<ReservaResponseDTO> crear(@Valid @RequestBody ReservaRequestDTO body) {
        return new ResponseEntity<>(service.crear(body), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ReservaResponseDTO actualizar(@PathVariable Integer id, @Valid @RequestBody ReservaRequestDTO body) {
        return service.actualizar(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}