package com.patiperro.pagos.repository;

import com.patiperro.pagos.model.BilleteraReservaFase;
import com.patiperro.pagos.model.BilleteraReservaTracking;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BilleteraReservaTrackingRepository extends JpaRepository<BilleteraReservaTracking, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM BilleteraReservaTracking t WHERE t.idTracking = :idTracking")
    Optional<BilleteraReservaTracking> findByIdTrackingForUpdate(@Param("idTracking") Long idTracking);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM BilleteraReservaTracking t WHERE t.idReserva = :idReserva")
    Optional<BilleteraReservaTracking> findByIdReservaForUpdate(@Param("idReserva") Integer idReserva);

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
