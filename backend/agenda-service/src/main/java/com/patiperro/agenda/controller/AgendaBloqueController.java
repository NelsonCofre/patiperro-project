package com.patiperro.agenda.controller;

import com.patiperro.agenda.dto.AgendaBloqueRequestDTO;
import com.patiperro.agenda.dto.AgendaBloqueResponseDTO;
import com.patiperro.agenda.dto.AgendaBloqueSerieMensualRequestDTO;
import com.patiperro.agenda.dto.AgendaBloqueSerieMensualResponseDTO;
import com.patiperro.agenda.service.AgendaBloqueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

<<<<<<< Updated upstream
        /**
     * Bloques del paseador en el rango (oferta para tutores / reservas).
     * No incluye franjas en fechas con bloqueo personal de día completo ({@code agenda_bloqueo_dia}).
     */
=======
>>>>>>> Stashed changes
    @GetMapping("/usuario/{idUsuario}/oferta")
    public List<AgendaBloqueResponseDTO> listarBloquesOfertables(
            @PathVariable Integer idUsuario,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.listarBloquesOfertables(idUsuario, desde, hasta);
    }

        /**
     * IDs de usuario (paseadores) con bloque disponible que solapa la franja en {@code fecha},
     * excluyendo quienes tengan ese día bloqueado por motivos personales.
     */
    @GetMapping("/busqueda/disponibles")
    public List<Integer> buscarPaseadoresDisponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime horaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime horaFin,
            @RequestParam Integer idEstadoDisponible) {
        return service.buscarIdUsuariosDisponiblesEnFranja(fecha, horaInicio, horaFin, idEstadoDisponible);
    }

    /**
     * IDs de usuario (paseadores) con al menos un bloque disponible desde la fecha de referencia
     * (por defecto hoy). Query param opcional {@code desdeFecha} para pruebas o zona explícita.
     */
    @GetMapping("/busqueda/disponibles-desde-hoy")
    public List<Integer> buscarPaseadoresDisponiblesDesdeHoy(
            @RequestParam Integer idEstadoDisponible,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desdeFecha) {
        LocalDate desde = desdeFecha != null ? desdeFecha : LocalDate.now();
        return service.buscarIdUsuariosDisponiblesDesdeFecha(desde, idEstadoDisponible);
    }

    @GetMapping("/{id}")
    public AgendaBloqueResponseDTO obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<AgendaBloqueResponseDTO> crear(@Valid @RequestBody AgendaBloqueRequestDTO body) {
        return new ResponseEntity<>(service.crear(body), HttpStatus.CREATED);
    }

    @PostMapping("/serie-mes")
    public ResponseEntity<AgendaBloqueSerieMensualResponseDTO> crearSerieMensual(
            @Valid @RequestBody AgendaBloqueSerieMensualRequestDTO body) {
        return new ResponseEntity<>(service.crearSerieMensualEnMes(body), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public AgendaBloqueResponseDTO actualizar(@PathVariable Integer id, @Valid @RequestBody AgendaBloqueRequestDTO body) {
        return service.actualizar(id, body);
    }

    @PatchMapping("/{id}/marcar-reservado")
    public AgendaBloqueResponseDTO marcarReservado(
            @PathVariable Integer id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.marcarReservado(id, rawJwt(authorization));
    }

    @PatchMapping("/{id}/marcar-disponible")
    public AgendaBloqueResponseDTO marcarDisponible(
            @PathVariable Integer id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.marcarDisponible(id, rawJwt(authorization));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private static String rawJwt(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Se requiere Authorization Bearer");
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token vacío");
        }
        return token;
    }
}
