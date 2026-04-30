package com.patiperro.agenda.controller;

import com.patiperro.agenda.dto.AgendaBloqueResponseDTO;
import com.patiperro.agenda.service.AgendaBloqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operaciones servidor-a-servidor (p. ej. reserva-service tras cancelación del tutor).
 */
@RestController
@RequestMapping("/api/agenda/interno/bloques")
@RequiredArgsConstructor
public class AgendaBloqueInternoController {

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private final AgendaBloqueService service;

    /**
     * Lectura servidor-a-servidor de un bloque (p. ej. para resolver el paseador dueño del bloque).
     */
    @GetMapping("/{id}")
    public AgendaBloqueResponseDTO obtenerInterno(
            @PathVariable Integer id,
            @RequestHeader(value = HEADER_INTERNO, required = false) String secret) {
        return service.obtenerInterno(id, secret);
    }

    @PatchMapping("/{id}/marcar-disponible")
    public AgendaBloqueResponseDTO marcarDisponible(
            @PathVariable Integer id,
            @RequestHeader(value = HEADER_INTERNO, required = false) String secret) {
        return service.marcarDisponibleInterno(id, secret);
    }
}
