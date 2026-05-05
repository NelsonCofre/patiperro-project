package com.patiperro.pagos.repository;

import com.patiperro.pagos.model.Billetera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BilleteraRepository extends JpaRepository<Billetera, Long> {

    Optional<Billetera> findByIdUsuario(Long idUsuario);
}
