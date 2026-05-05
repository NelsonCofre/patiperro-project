package com.patiperro.pagos.repository;

import com.patiperro.pagos.model.BilleteraReservaFase;
import com.patiperro.pagos.model.BilleteraReservaTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BilleteraReservaTrackingRepository extends JpaRepository<BilleteraReservaTracking, Long> {

    Optional<BilleteraReservaTracking> findByIdReserva(Integer idReserva);

    List<BilleteraReservaTracking> findByIdUsuarioPaseadorAndFase(Long idUsuarioPaseador, BilleteraReservaFase fase);

    List<BilleteraReservaTracking> findByFaseAndLiberadoEnIsNull(BilleteraReservaFase fase);
}
