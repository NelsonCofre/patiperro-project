package com.patiperro.mascota.repository;

import com.patiperro.mascota.model.Raza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RazaRepository extends JpaRepository<Raza, Long> {

    List<Raza> findByEspecie_IdEspecie(Long idEspecie);
}