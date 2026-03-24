package com.patiperro.tutores.user.repository;

import com.patiperro.tutores.user.model.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TutorRepository extends JpaRepository<Tutor, Long> {

    // Query derivada: busca un tutor por correo (usada en login).
    Optional<Tutor> findByCorreo(String correo);

    // Query derivada: valida existencia de correo (usada en register).
    boolean existsByCorreo(String correo);
}
