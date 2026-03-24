package com.patiperro.tutores.user.repository;

import com.patiperro.tutores.user.model.Direccion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DireccionRepository extends JpaRepository<Direccion, Long> {
    // De momento se usan operaciones CRUD base heredadas de JpaRepository.
}
