package com.patiperro.paseador.repository;

import com.patiperro.paseador.model.Foto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FotoRepository extends JpaRepository<Foto, Long> {

    List<Foto> findByPaseador_Id(Long paseadorId);
}
