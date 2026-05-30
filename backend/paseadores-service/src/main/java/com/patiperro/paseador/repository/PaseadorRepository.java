package com.patiperro.paseador.repository;

import com.patiperro.paseador.model.Paseador;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaseadorRepository extends JpaRepository<Paseador, Long> {

    Optional<Paseador> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    /** Evita doble subida concurrente mientras se actualiza el estado de verificación. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Paseador p WHERE p.id = :id")
    Optional<Paseador> findByIdForUpdate(@Param("id") Long id);
}
