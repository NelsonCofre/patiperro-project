package com.patiperro.mascota.repository;

import com.patiperro.mascota.model.Tamano;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TamanoRepository extends JpaRepository<Tamano, Long> {
}
