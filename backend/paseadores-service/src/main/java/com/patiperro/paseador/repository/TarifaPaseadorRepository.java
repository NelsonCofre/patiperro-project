package com.patiperro.paseador.repository;

import com.patiperro.paseador.model.TarifaPaseador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TarifaPaseadorRepository extends JpaRepository<TarifaPaseador, Long> {

    List<TarifaPaseador> findByConfiguracion_Id(Long configuracionId);

    // AGREGA ESTO:
    @Query("SELECT MIN(t.precioBase) FROM TarifaPaseador t WHERE t.configuracion.paseador.id = :idPaseador")
    Integer findTarifaMinimaByPaseadorId(@Param("idPaseador") Long idPaseador);
}
