package com.patiperro.paseador.repository;

import com.patiperro.paseador.model.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionRepository extends JpaRepository<Configuracion, Long> {

    Optional<Configuracion> findByPaseador_Id(Long paseadorId);
}
