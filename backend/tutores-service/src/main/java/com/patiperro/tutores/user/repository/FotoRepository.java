package com.patiperro.tutores.user.repository;

import com.patiperro.tutores.user.model.Foto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FotoRepository extends JpaRepository<Foto, Long> {

    // Query derivada: retorna todas las fotos asociadas a un tutor por su id.
    List<Foto> findByTutor_Id(Long tutorId);
}
