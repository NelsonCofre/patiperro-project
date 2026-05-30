package com.patiperro.pagos.repository;

import com.patiperro.pagos.model.RetiroFondo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RetiroFondoRepository extends JpaRepository<RetiroFondo, Long> {

    @Query(
            """
            SELECT r FROM RetiroFondo r
            JOIN FETCH r.transaccion t
            WHERE r.idUsuarioPaseador = :idUsuario
            ORDER BY r.creadoEn DESC
            """)
    List<RetiroFondo> findHistorialByIdUsuarioPaseador(
            @Param("idUsuario") Long idUsuarioPaseador,
            Pageable pageable);
}
