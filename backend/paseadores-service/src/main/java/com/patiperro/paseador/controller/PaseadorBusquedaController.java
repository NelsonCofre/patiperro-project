package com.patiperro.paseador.controller;

import com.patiperro.paseador.dto.user.PaseadorPerfilDTO;
import com.patiperro.paseador.user.dto.PaseadorCercanoResponseDTO;
import com.patiperro.paseador.user.dto.PaseadorCercanosConConteoResponseDTO;
import com.patiperro.paseador.user.service.PaseadorBusquedaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/paseadores/public")
@RequiredArgsConstructor
public class PaseadorBusquedaController {   

    private final PaseadorBusquedaService paseadorBusquedaService;

    /**
     * Búsqueda pública de paseadores cercanos.
     *
     * Modos soportados:
     * - Solo geográfico: latitudReferencia, longitudReferencia, radioBusquedaMaxKm (opcional), limite (opcional).
     * - Geográfico + disponibilidad (agenda-service): se deben indicar los 4 parámetros de agenda
     *   (fechaDisponibilidad, horaInicioDisponibilidad, horaFinDisponibilidad, idEstadoBloqueDisponible).
     *
     * El radio efectivo es {@code LEAST(radio_cobertura_del_paseador, radioBusquedaMaxKm)}.
     */
    @GetMapping("/cercanos")
    public List<PaseadorCercanoResponseDTO> listarCercanos(
            @RequestParam double latitudReferencia,
            @RequestParam double longitudReferencia,
            @RequestParam(defaultValue = "10") double radioBusquedaMaxKm,
            @RequestParam(defaultValue = "20") int limite,
            @RequestParam(required = false) LocalDate fechaDisponibilidad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime horaInicioDisponibilidad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime horaFinDisponibilidad,
            @RequestParam(required = false) Integer idEstadoBloqueDisponible) {
        return paseadorBusquedaService.buscarCercanos(
                latitudReferencia,
                longitudReferencia,
                radioBusquedaMaxKm,
                limite,
                fechaDisponibilidad,
                horaInicioDisponibilidad,
                horaFinDisponibilidad,
                idEstadoBloqueDisponible);
    }

    /**
     * Variante con conteo real + paginación por offset/limit.
     * Mantiene el endpoint /cercanos intacto para compatibilidad con el frontend actual.
     */
    @GetMapping("/cercanos-con-conteo")
    public PaseadorCercanosConConteoResponseDTO listarCercanosConConteo(
            @RequestParam double latitudReferencia,
            @RequestParam double longitudReferencia,
            @RequestParam(defaultValue = "10") double radioBusquedaMaxKm,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(name = "limite", required = false) Integer limite,
            @RequestParam(required = false) LocalDate fechaDisponibilidad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime horaInicioDisponibilidad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime horaFinDisponibilidad,
            @RequestParam(required = false) Integer idEstadoBloqueDisponible) {
        int limitEfectivo = limite != null ? limite : limit;
        return paseadorBusquedaService.buscarCercanosConConteo(
                latitudReferencia,
                longitudReferencia,
                radioBusquedaMaxKm,
                offset,
                limitEfectivo,
                fechaDisponibilidad,
                horaInicioDisponibilidad,
                horaFinDisponibilidad,
                idEstadoBloqueDisponible);
    }

    // En algún controlador con @RequestMapping("/api/paseadores/public")

/**
     * Búsqueda pública del perfil básico de un paseador por su ID.
     * Esto es usado por el Tutor para obtener el correo y enviar notificaciones.
     */
    @GetMapping("/{id}")
    public PaseadorPerfilDTO obtenerPerfilPaseador(@PathVariable Long id) {
        return paseadorBusquedaService.obtenerPerfilPorId(id);
    }
}
