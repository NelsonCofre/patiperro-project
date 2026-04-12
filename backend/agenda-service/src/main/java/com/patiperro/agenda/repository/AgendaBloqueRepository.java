package com.patiperro.agenda.repository;

import com.patiperro.agenda.model.AgendaBloque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgendaBloqueRepository extends JpaRepository<AgendaBloque, Integer> {

    List<AgendaBloque> findByIdUsuario(Integer idUsuario);

    List<AgendaBloque> findByIdUsuarioAndFechaBetweenOrderByFechaAscHoraInicioAsc(
            Integer idUsuario, LocalDate desde, LocalDate hasta);

    @Query(
            """
            SELECT DISTINCT b.idUsuario FROM AgendaBloque b
            WHERE b.fecha = :fecha
              AND b.estadoBloque.idEstado = :idEstadoDisponible
              AND b.horaInicio < :finBuscado
              AND b.horaFinal > :inicioBuscado
              AND b.idUsuario NOT IN (
                SELECT bd.idUsuario FROM AgendaBloqueoDia bd WHERE bd.fecha = :fecha
              )
            """)
    List<Integer> findIdUsuariosConBloqueDisponibleEnFranja(
            @Param("fecha") LocalDate fecha,
            @Param("inicioBuscado") LocalDateTime inicioBuscado,
            @Param("finBuscado") LocalDateTime finBuscado,
            @Param("idEstadoDisponible") Integer idEstadoDisponible);

    // =========================================================================
    // MÉTODO NUEVO: Obtener solo IDs para validación cruzada
    // =========================================================================
    /**
     * Obtiene una lista de los identificadores (PK) de los bloques de un usuario
     * en un rango de fechas. Es útil para enviarlos al reserva-service y
     * verificar compromisos antes de borrar o bloquear días.
     */
    @Query("""
        SELECT b.idAgenda FROM AgendaBloque b
        WHERE b.idUsuario = :idUsuario
          AND b.fecha BETWEEN :desde AND :hasta
        """)
    List<Integer> findIdsByIdUsuarioAndFechaBetween(
            @Param("idUsuario") Integer idUsuario,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}
