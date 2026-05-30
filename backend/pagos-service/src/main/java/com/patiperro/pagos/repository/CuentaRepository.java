package com.patiperro.pagos.repository;

import com.patiperro.pagos.model.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByBilletera_IdBilletera(Long idBilletera);

    @Query(
            "select c from Cuenta c join fetch c.banco join fetch c.tipoCuenta "
                    + "where c.billetera.idBilletera = :idBilletera")
    Optional<Cuenta> findWithRelationsByBilleteraId(@Param("idBilletera") Long idBilletera);
}

