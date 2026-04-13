package com.patiperro.agenda.controller;

import com.patiperro.agenda.dto.AgendaBloqueoDiaRequestDTO;
import com.patiperro.agenda.dto.AgendaBloqueoDiaResponseDTO;
import com.patiperro.agenda.service.AgendaBloqueoDiaService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

@RestController
@RequestMapping("/api/agenda/bloqueos-dia")
@RequiredArgsConstructor
public class AgendaBloqueoDiaController {

    private final AgendaBloqueoDiaService service;

    @GetMapping
    public List<AgendaBloqueoDiaResponseDTO> listar() {
        return service.listar();
    }

    @GetMapping("/usuario/{idUsuario}")
    public List<AgendaBloqueoDiaResponseDTO> listarPorUsuario(@PathVariable Integer idUsuario) {
        return service.listarPorUsuario(idUsuario);
    }

    @GetMapping("/usuario/{idUsuario}/rango")
    public List<AgendaBloqueoDiaResponseDTO> listarPorUsuarioYRango(
            @PathVariable Integer idUsuario,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.listarPorUsuarioYRango(idUsuario, desde, hasta);
    }

    @GetMapping("/{id}")
    public AgendaBloqueoDiaResponseDTO obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<AgendaBloqueoDiaResponseDTO> crear(@Valid @RequestBody AgendaBloqueoDiaRequestDTO body) {
        return new ResponseEntity<>(service.crear(body), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public AgendaBloqueoDiaResponseDTO actualizar(
            @PathVariable Integer id, @Valid @RequestBody AgendaBloqueoDiaRequestDTO body) {
        return service.actualizar(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** Desbloqueo por día: elimina el registro de bloqueo personal para ese usuario y fecha. */
    @DeleteMapping("/usuario/{idUsuario}/fecha/{fecha}")
    public ResponseEntity<Void> eliminarPorUsuarioYFecha(
            @PathVariable Integer idUsuario,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        service.eliminarPorUsuarioYFecha(idUsuario, fecha);
        return ResponseEntity.noContent().build();
    }
}
