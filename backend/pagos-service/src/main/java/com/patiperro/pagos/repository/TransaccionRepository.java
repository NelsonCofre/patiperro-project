package com.patiperro.pagos.repository;

import com.patiperro.pagos.model.EstadoPago;
import com.patiperro.pagos.model.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    Optional<Transaccion> findFirstByIdReservaAndEstadoPagoOrderByIdTransaccionDesc(Long idReserva, EstadoPago estadoPago);
}
