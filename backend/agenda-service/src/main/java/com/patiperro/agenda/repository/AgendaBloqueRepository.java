package com.patiperro.agenda.repository;

import com.patiperro.agenda.model.AgendaBloque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgendaBloqueRepository extends JpaRepository<AgendaBloque, Integer> {

    List<AgendaBloque> findByIdUsuario(Integer idUsuario);
}
