package com.patiperro.agenda.repository;

import com.patiperro.agenda.model.AgendaBloqueoDia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AgendaBloqueoDiaRepository extends JpaRepository<AgendaBloqueoDia, Integer> {

    List<AgendaBloqueoDia> findByIdUsuarioOrderByFechaAsc(Integer idUsuario);

    List<AgendaBloqueoDia> findByIdUsuarioAndFechaBetweenOrderByFechaAsc(
            Integer idUsuario, LocalDate desde, LocalDate hasta);

    boolean existsByIdUsuarioAndFecha(Integer idUsuario, LocalDate fecha);

    Optional<AgendaBloqueoDia> findByIdUsuarioAndFecha(Integer idUsuario, LocalDate fecha);
}
