package com.patiperro.pagos.repository;

import com.patiperro.pagos.model.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByBilletera_IdBilletera(Long idBilletera);
}

