package com.patiperro.mascota.repository;

import com.patiperro.mascota.model.MascotaFoto;
import com.patiperro.mascota.model.MascotaFotoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MascotaFotoRepository extends JpaRepository<MascotaFoto, MascotaFotoId> {

    List<MascotaFoto> findByMascota_IdMascota(Long idMascota);

    long countByFoto_IdFoto(Long idFoto);
}
