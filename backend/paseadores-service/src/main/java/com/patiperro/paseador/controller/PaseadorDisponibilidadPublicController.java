package com.patiperro.paseador.controller;

import com.patiperro.paseador.dto.disponibilidad.PaseadorDisponibilidadResponseDTO;
import com.patiperro.paseador.user.service.PaseadorDisponibilidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/paseadores/public")
@RequiredArgsConstructor
public class PaseadorDisponibilidadPublicController {

    private final PaseadorDisponibilidadService disponibilidadService;

    /**
     * Bloques disponibles del paseador agrupados por fecha (ventana desde hoy).
     * Ejemplo: {@code GET /api/paseadores/public/10/disponibilidad?dias=7}
     */
    @GetMapping("/{idPaseador}/disponibilidad")
    public PaseadorDisponibilidadResponseDTO disponibilidad(
            @PathVariable long idPaseador,
            @RequestParam(name = "dias", defaultValue = "7") int dias) {
        return disponibilidadService.disponibilidadProximosDias(idPaseador, dias);
    }
}
