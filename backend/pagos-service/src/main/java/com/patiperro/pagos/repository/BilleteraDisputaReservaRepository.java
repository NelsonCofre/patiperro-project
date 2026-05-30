package com.patiperro.pagos.repository;

import com.patiperro.pagos.model.BilleteraDisputaReserva;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BilleteraDisputaReservaRepository extends JpaRepository<BilleteraDisputaReserva, Integer> {

    boolean existsByIdReservaAndDisputaActivaTrue(Integer idReserva);
}
