package com.patiperro.mascota.repository;

import com.patiperro.mascota.model.Mascota;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MascotaRepository extends JpaRepository<Mascota, Long> {

    @EntityGraph(attributePaths = {"especie", "raza", "tamano"})
    List<Mascota> findByIdTutor(Long idTutor);

    @EntityGraph(attributePaths = {"especie", "raza", "tamano", "mascotaFotos", "mascotaFotos.foto"})
    @Query("SELECT m FROM Mascota m WHERE m.idMascota = :id")
    Optional<Mascota> findPerfilById(@Param("id") Long id);
}
