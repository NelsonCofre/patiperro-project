package com.patiperro.pagos.repository;

import com.patiperro.pagos.model.ComprobantePagoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComprobantePagoRegistroRepository extends JpaRepository<ComprobantePagoRegistro, Long> {

    Optional<ComprobantePagoRegistro> findByIdReserva(Integer idReserva);
}
