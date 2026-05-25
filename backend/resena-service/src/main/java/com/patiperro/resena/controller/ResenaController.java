package com.patiperro.resena.controller;

import com.patiperro.resena.dto.ResenaDetalleDTO;
import com.patiperro.resena.model.Resena;
import com.patiperro.resena.service.ResenaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resenas")
public class ResenaController {

    @Autowired
    private ResenaService resenaService;

    /**
     * Guarda una reseña con validación manual.
     * Criterio: Estrellas obligatorias (1-5), comentario opcional (<250 caracteres)
     * e IDs de Reserva, Paseador y Tutor obligatorios.
     */
    @PostMapping
    public ResponseEntity<?> crearResena(@RequestBody Resena resena) {
        
        // 1. Validación manual de estrellas (TAPG3-242)
        if (resena.getEstrellas() == null || resena.getEstrellas() < 1 || resena.getEstrellas() > 5) {
            return ResponseEntity.badRequest()
                .body("Error: La calificación es obligatoria y debe estar entre 1 y 5 estrellas.");
        }

        // 2. Validación manual de comentario (TAPG3-243)
        if (resena.getComentario() != null && resena.getComentario().length() > 250) {
            return ResponseEntity.badRequest()
                .body("Error: El comentario no puede superar los 250 caracteres.");
        }

        // 3. Validación de IDs obligatorios (Incluyendo idTutor)
        if (resena.getIdReserva() == null || resena.getIdPaseador() == null || resena.getIdTutor() == null) {
            return ResponseEntity.badRequest()
                .body("Error: Los IDs de reserva, paseador y tutor son obligatorios para registrar la reseña.");
        }

        try {
            // Intenta guardar usando la lógica del Service (que valida duplicados TAPG3-245)
            Resena guardada = resenaService.guardarResena(resena);
            return new ResponseEntity<>(guardada, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            // Si el service lanza error por reserva ya calificada
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /**
     * Lista todas las reseñas registradas en el sistema.
     */
    @GetMapping
    public ResponseEntity<List<Resena>> listarTodas() {
        return ResponseEntity.ok(resenaService.buscarTodas());
    }

    /**
     * Reservas que el tutor ya calificó (evita re-calificar tras refrescar la página).
     */
    @GetMapping("/tutor/{idTutor}/reservas-calificadas")
    public ResponseEntity<List<Integer>> listarReservasCalificadasPorTutor(@PathVariable Long idTutor) {
        return ResponseEntity.ok(resenaService.listarIdsReservaCalificadasPorTutor(idTutor));
    }

    /**
     * Obtiene el historial de reseñas de un paseador específico.
     */
    @GetMapping("/paseador/{idPaseador}")
public ResponseEntity<List<ResenaDetalleDTO>> listarPorPaseador(@PathVariable Integer idPaseador) {
    // Cambiamos el retorno de Resena a ResenaDetalleDTO
    return ResponseEntity.ok(resenaService.buscarPorPaseador(idPaseador));
}

    /**
     * Devuelve la calificación promedio (Reputación) del paseador (TAPG3-244).
     */
    @GetMapping("/paseador/{idPaseador}/promedio")
    public ResponseEntity<Double> obtenerPromedio(@PathVariable Integer idPaseador) {
        Double promedio = resenaService.obtenerPromedioPaseador(idPaseador);
        return ResponseEntity.ok(promedio);
    }

    /**
     * Elimina una reseña por su ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        resenaService.eliminarResena(id);
        return ResponseEntity.noContent().build();
    }
}