package com.patiperro.resena.service;

import com.patiperro.resena.client.ReservaClient;
import com.patiperro.resena.client.TutorClient;
import com.patiperro.resena.dto.ResenaDetalleDTO;
import com.patiperro.resena.dto.ReservaDTO;
import com.patiperro.resena.dto.TutorDTO;
import com.patiperro.resena.model.Resena;
import com.patiperro.resena.repository.ResenaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ResenaService {

    @Autowired
    private ResenaRepository resenaRepository;

    @Autowired
    private ReservaClient reservaClient;

    @Autowired
    private TutorClient tutorClient;

    /**
     * Guarda una nueva reseña validando:
     * 1. Que la reserva no haya sido calificada previamente (TAPG3-245).
     * 2. Que el paseo esté en estado FINALIZADA (Integración inter-servicio).
     */
    @Transactional
    public Resena guardarResena(Resena resena) {
        // 1. Validar duplicados en la base de datos local
        if (resenaRepository.existsByIdReserva(resena.getIdReserva())) {
            throw new RuntimeException("Error: Esta reserva ya ha sido calificada. No se permiten duplicados.");
        }

        // 2. Validación inter-servicio: Llamada al reserva-service
        try {
            ReservaDTO reserva = reservaClient.obtenerReservaPorId(resena.getIdReserva());
            if (reserva == null || !"FINALIZADA".equalsIgnoreCase(reserva.getNombreEstado())) {
                throw new RuntimeException("Error: Solo se pueden calificar paseos con estado FINALIZADA.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error: No se pudo verificar el estado de la reserva con el servicio externo.");
        }

        return resenaRepository.save(resena);
    }

    /**
     * Busca una reseña por ID y la transforma en un DTO con los datos reales del Tutor
     * (Nombre, Apellido y Foto) obtenidos del tutor-service.
     */
    public ResenaDetalleDTO obtenerResenaCompleta(Integer id) {
        Resena resena = resenaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reseña no encontrada."));

        // Llamada al microservicio de tutores para enriquecer la información
        TutorDTO tutor = tutorClient.obtenerTutorPorId(resena.getIdTutor());

        return ResenaDetalleDTO.builder()
                .id(resena.getId())
                .estrellas(resena.getEstrellas())
                .comentario(resena.getComentario())
                .nombreTutor(tutor.getNombre() + " " + tutor.getApellido())
                .idReserva(resena.getIdReserva())
                .build();
    }

    /**
     * Lista todas las reseñas registradas.
     */
    public List<Resena> buscarTodas() {
        return resenaRepository.findAll();
    }

    /**
     * Busca las reseñas de un paseador específico.
     */
    public List<ResenaDetalleDTO> buscarPorPaseador(Integer idPaseador) {
    List<Resena> resenas = resenaRepository.findByIdPaseador(idPaseador);

    // Dentro del stream en ResenaService.java
return resenas.stream().map(resena -> {
    String nombreFinal = "Usuario de Patiperro";

    try {
        TutorDTO tutor = tutorClient.obtenerTutorPorId(resena.getIdTutor());
        
        if (tutor != null && tutor.getNombre() != null) {
            // Sebastian + espacio + Ulloa
            String nombre = tutor.getNombre();
            String apellido = (tutor.getApellido() != null) ? tutor.getApellido() : "";
            nombreFinal = (nombre + " " + apellido).trim();
        }
    } catch (Exception e) {
        System.err.println("Error al obtener tutor " + resena.getIdTutor() + ": " + e.getMessage());
    }

    return ResenaDetalleDTO.builder()
            .id(resena.getId())
            .nombreTutor(nombreFinal)
            .estrellas(resena.getEstrellas())
            .comentario(resena.getComentario())
            .idReserva(resena.getIdReserva())
            .build();
}).collect(Collectors.toList());
}

    /**
     * Calcula el promedio de estrellas de un paseador (TAPG3-244).
     */
    public Double obtenerPromedioPaseador(Integer idPaseador) {
        List<Resena> resenas = resenaRepository.findByIdPaseador(idPaseador);
        
        if (resenas.isEmpty()) {
            return 0.0;
        }

        return resenas.stream()
                .mapToDouble(Resena::getEstrellas)
                .average()
                .orElse(0.0);
    }

    /**
     * Elimina una reseña (Uso administrativo).
     */
    @Transactional
    public void eliminarResena(Integer id) {
        resenaRepository.deleteById(id);
    }
}