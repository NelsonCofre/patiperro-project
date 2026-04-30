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
     * Otra reserva "activa" (estados en curso de uso del código) con el mismo {@code codigoEncuentro}.
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reserva r "
            + "WHERE r.codigoEncuentro = :codigo AND r.idReserva <> :excludeId AND r.estadoReserva.idEstadoReserva IN :estados")
    boolean existsOtraActivaConCodigo(
            @Param("codigo") Integer codigo,
            @Param("excludeId") Integer excludeId,
            @Param("estados") Collection<Integer> estados);

    /**
     * Pasa a EN_CURSO, fija inicio real y limpia contador/bloqueo, solo si sigue ACEPTADA (una fila, atómico).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Reserva r SET r.estadoReserva = :enCurso, r.fechaInicioReal = CURRENT_TIMESTAMP, r.codigoIntentosFallidos = 0, r.codigoBloqueadoHasta = null "
            + "WHERE r.idReserva = :idReserva AND r.estadoReserva.idEstadoReserva = :idAceptada")
    int marcarEnCursoTrasValidarCodigo(
            @Param("enCurso") EstadoReserva enCurso,
            @Param("idReserva") Integer idReserva,
            @Param("idAceptada") Integer idAceptada);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Reserva r SET r.codigoIntentosFallidos = COALESCE(r.codigoIntentosFallidos, 0) + 1 WHERE r.idReserva = :idReserva")
    int incrementarIntentosFallidosCodigo(@Param("idReserva") Integer idReserva);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Reserva r SET r.codigoBloqueadoHasta = :hasta WHERE r.idReserva = :idReserva")
    int fijarBloqueoCodigoHasta(@Param("idReserva") Integer idReserva, @Param("hasta") LocalDateTime hasta);

    /**
     * Marca la reserva como PAGADA de forma atómica, solo si está en uno de los estados origen permitidos.
     * Útil para idempotencia ante webhooks duplicados y para evitar condiciones de carrera.
     *
     * @return cantidad de filas actualizadas (0 o 1)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Reserva r SET r.estadoReserva = :pagada "
            + "WHERE r.idReserva = :idReserva AND r.estadoReserva.idEstadoReserva IN :idsOrigen")
    int marcarPagadaSiEstadoEn(
            @Param("pagada") EstadoReserva pagada,
            @Param("idReserva") Integer idReserva,
            @Param("idsOrigen") Collection<Integer> idsOrigen);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Reserva r SET r.codigoIntentosFallidos = 0, r.codigoBloqueadoHasta = null WHERE r.idReserva = :idReserva")
    int reiniciarContadoresBloqueoCodigo(@Param("idReserva") Integer idReserva);

    /**
     * ACEPTADA, sin paseo iniciado, con PIN y sin expiración calculada: backfill (listado / job, sin JWT).
     */
    @Query("SELECT r.idReserva FROM Reserva r "
            + "WHERE r.estadoReserva.idEstadoReserva = :idAceptada AND r.fechaInicioReal IS NULL AND "
            + "r.codigoEncuentro IS NOT NULL AND r.codigoEncuentroExpiraEn IS NULL "
            + "ORDER BY r.idReserva")
    List<Integer> findIdReservasAceptadaConCodigoSinExpiracion(
            @Param("idAceptada") Integer idAceptada);

    /**
     * ACEPTADA, sin inicio de paseo, con ventana de PIN ya vencida: regenerar PIN automáticamente.
     */
    @Query("SELECT r.idReserva FROM Reserva r "
            + "WHERE r.estadoReserva.idEstadoReserva = :idAceptada AND r.fechaInicioReal IS NULL AND "
            + "r.codigoEncuentro IS NOT NULL AND r.codigoEncuentroExpiraEn IS NOT NULL AND "
            + "r.codigoEncuentroExpiraEn < :ahora "
            + "ORDER BY r.idReserva")
    List<Integer> findIdReservasAceptadaParaRegenerarCodigoPorEncuentroVencido(
            @Param("idAceptada") Integer idAceptada,
            @Param("ahora") LocalDateTime ahora);
}
