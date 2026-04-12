package com.patiperro.paseador.controller;

import com.patiperro.paseador.user.dto.PaseadorCercanoResponseDTO;
import com.patiperro.paseador.user.service.PaseadorBusquedaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
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
     * Paseadores cuyo punto (direccion) esta dentro de {@code LEAST(radio_cobertura, radioBusquedaMaxKm)} km
     * respecto al punto de referencia. Opcionalmente filtra por disponibilidad en agenda-service.
     */
    @GetMapping("/cercanos")
    public List<PaseadorCercanoResponseDTO> listarCercanos(
            @RequestParam double latitudReferencia,
            @RequestParam double longitudReferencia,
            @RequestParam(defaultValue = "50") double radioBusquedaMaxKm,
            @RequestParam(defaultValue = "50") int limite,
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
}
