package com.patiperro.agenda.controller;

import com.patiperro.agenda.dto.AgendaBloqueResponseDTO;
import com.patiperro.agenda.service.AgendaBloqueService;
import lombok.RequiredArgsConstructor;
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

    @PatchMapping("/{id}/marcar-disponible")
    public AgendaBloqueResponseDTO marcarDisponible(
            @PathVariable Integer id,
            @RequestHeader(value = HEADER_INTERNO, required = false) String secret) {
        return service.marcarDisponibleInterno(id, secret);
    }
}
