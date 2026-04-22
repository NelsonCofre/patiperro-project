package com.patiperro.reserva.repository;

import com.patiperro.reserva.model.EstadoReserva;
import com.patiperro.reserva.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    List<Reserva> findByIdTutorUsuario(Integer idTutorUsuario);

    List<Reserva> findByIdMascota(Integer idMascota);

    List<Reserva> findByIdAgendaBloque(Integer idAgendaBloque);

    List<Reserva> findByEstadoReserva_IdEstadoReserva(Integer idEstadoReserva);

    /**
     * VIGILANCIA DE COMPROMISOS:
     * Verifica si alguno de los bloques de agenda enviados ya tiene una reserva 
     * en estados que impiden cambios (ej. Confirmada, Pagada, En Curso).
     * 
     * @param idsAgenda Lista de IDs de bloques a revisar.
     * @param idsEstadoReservaComprometidos IDs de los estados que bloquean la edición.
     * @return true si existe al menos una coincidencia (hay un compromiso previo).
     */
    boolean existsByIdAgendaBloqueInAndEstadoReserva_IdEstadoReservaIn(
            Collection<Integer> idsAgenda,
            Collection<Integer> idsEstadoReservaComprometidos);

    List<Reserva> findByIdAgendaBloqueInAndEstadoReserva_IdEstadoReserva(
            Collection<Integer> idsAgendaBloque,
            Integer idEstadoReserva);

    List<Reserva> findByIdAgendaBloqueInAndEstadoReserva_IdEstadoReservaIn(
            Collection<Integer> idsAgendaBloque,
            Collection<Integer> idsEstadoReserva);

    /**
     * Pasa a EN_CURSO, fija inicio real y limpia contador/bloqueo, solo si sigue ACEPTADA (una fila, atómico).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Reserva r SET r.estadoReserva = :enCurso, r.fechaInicioReal = :inicio, r.codigoIntentosFallidos = 0, r.codigoBloqueadoHasta = null "
            + "WHERE r.idReserva = :idReserva AND r.estadoReserva.idEstadoReserva = :idAceptada")
    int marcarEnCursoTrasValidarCodigo(
            @Param("enCurso") EstadoReserva enCurso,
            @Param("inicio") LocalDateTime inicio,
            @Param("idReserva") Integer idReserva,
            @Param("idAceptada") Integer idAceptada);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Reserva r SET r.codigoIntentosFallidos = COALESCE(r.codigoIntentosFallidos, 0) + 1 WHERE r.idReserva = :idReserva")
    int incrementarIntentosFallidosCodigo(@Param("idReserva") Integer idReserva);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Reserva r SET r.codigoBloqueadoHasta = :hasta WHERE r.idReserva = :idReserva")
    int fijarBloqueoCodigoHasta(@Param("idReserva") Integer idReserva, @Param("hasta") LocalDateTime hasta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Reserva r SET r.codigoIntentosFallidos = 0, r.codigoBloqueadoHasta = null WHERE r.idReserva = :idReserva")
    int reiniciarContadoresBloqueoCodigo(@Param("idReserva") Integer idReserva);
}
