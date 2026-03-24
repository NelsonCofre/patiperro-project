package com.patiperro.paseador.repository;

import com.patiperro.paseador.model.TarifaPaseador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TarifaPaseadorRepository extends JpaRepository<TarifaPaseador, Long> {

    List<TarifaPaseador> findByConfiguracion_Id(Long configuracionId);
}
