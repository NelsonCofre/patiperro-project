package com.patiperro.resena.repository;

import com.patiperro.resena.model.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ResenaRepository extends JpaRepository<Resena, Integer> {
    // Para validar si la reserva ya fue calificada
    boolean existsByIdReserva(Integer idReserva);
    
    // Para calcular el promedio del paseador
    java.util.List<Resena> findByIdPaseador(Integer idPaseador);
}