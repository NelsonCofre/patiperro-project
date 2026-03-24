package com.patiperro.paseador.repository;

import com.patiperro.paseador.model.Paseador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaseadorRepository extends JpaRepository<Paseador, Long> {

    Optional<Paseador> findByCorreo(String correo);

    boolean existsByCorreo(String correo);
}
