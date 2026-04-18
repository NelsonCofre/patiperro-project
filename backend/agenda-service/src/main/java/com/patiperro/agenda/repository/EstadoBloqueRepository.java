package com.patiperro.agenda.repository;

import com.patiperro.agenda.model.EstadoBloque;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadoBloqueRepository extends JpaRepository<EstadoBloque, Integer> {

    Optional<EstadoBloque> findByNombreIgnoreCase(String nombre);
}
