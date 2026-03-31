package com.patiperro.mascota.repository;

import com.patiperro.mascota.model.Foto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FotoRepository extends JpaRepository<Foto, Long> {
}
