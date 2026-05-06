package com.patiperro.pagos.repository;

import com.patiperro.pagos.model.BilleteraReservaFase;
import com.patiperro.pagos.model.BilleteraReservaTracking;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BilleteraReservaTrackingRepository extends JpaRepository<BilleteraReservaTracking, Long> {

    Optional<BilleteraReservaTracking> findByIdReserva(Integer idReserva);

    List<BilleteraReservaTracking> findByIdUsuarioPaseadorAndFase(Long idUsuarioPaseador, BilleteraReservaFase fase);

    List<BilleteraReservaTracking> findByFaseAndLiberadoEnIsNull(BilleteraReservaFase fase);

    /**
     * Historial reciente de liberaciones a disponible (conciliación). El límite va en {@link Pageable}
     * ({@code pageSize}); no define el saldo actual (retiros, etc.).
     */
    List<BilleteraReservaTracking> findByIdUsuarioPaseadorAndLiberadoEnIsNotNullOrderByLiberadoEnDesc(
            Long idUsuarioPaseador, Pageable pageable);
}
