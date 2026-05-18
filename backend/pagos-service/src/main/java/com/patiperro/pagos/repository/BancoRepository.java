package com.patiperro.pagos.repository;

import com.patiperro.pagos.model.Banco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BancoRepository extends JpaRepository<Banco, Long> {

    Optional<Banco> findByNombreIgnoreCase(String nombre);

    List<Banco> findAllByOrderByNombreAsc();
}
