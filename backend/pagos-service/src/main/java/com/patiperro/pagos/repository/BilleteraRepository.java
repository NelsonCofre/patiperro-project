package com.patiperro.pagos.repository;

import com.patiperro.pagos.model.Billetera;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BilleteraRepository extends JpaRepository<Billetera, Long> {

    Optional<Billetera> findByIdUsuario(Long idUsuario);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Billetera b WHERE b.idUsuario = :idUsuario")
    Optional<Billetera> findByIdUsuarioForUpdate(@Param("idUsuario") Long idUsuario);
}
