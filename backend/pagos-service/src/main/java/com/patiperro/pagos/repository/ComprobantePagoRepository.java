package com.patiperro.pagos.repository;

import com.patiperro.pagos.model.ComprobantePago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComprobantePagoRepository extends JpaRepository<ComprobantePago, Long> {
    Optional<ComprobantePago> findByIdReserva(Long idReserva);
}

