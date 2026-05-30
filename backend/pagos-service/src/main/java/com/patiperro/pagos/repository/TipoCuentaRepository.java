package com.patiperro.pagos.repository;

import com.patiperro.pagos.model.TipoCuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoCuentaRepository extends JpaRepository<TipoCuenta, Long> {

    Optional<TipoCuenta> findByNombreIgnoreCase(String nombre);

    List<TipoCuenta> findAllByOrderByNombreAsc();
}
