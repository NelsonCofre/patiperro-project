package com.patiperro.mascota.repository;

import com.patiperro.mascota.model.Foto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FotoRepository extends JpaRepository<Foto, Long> {

    List<Foto> findByMascota_IdMascota(Long idMascota);
}
