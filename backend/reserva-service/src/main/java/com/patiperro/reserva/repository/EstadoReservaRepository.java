package com.patiperro.reserva.repository;

import com.patiperro.reserva.model.EstadoReserva;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoReservaRepository extends JpaRepository<EstadoReserva, Integer> {

    Optional<EstadoReserva> findByNombreEstadoIgnoreCase(String nombreEstado);
}
