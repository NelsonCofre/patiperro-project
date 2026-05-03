package com.patiperro.reserva.repository;

import com.patiperro.reserva.model.EstadoReserva;
import com.patiperro.reserva.model.Reserva;
import org.springframework.data.domain.Pageable;
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
     * Buscar reserva por id de pago Mercado Pago (webhooks / soporte). Puede haber más de una fila si datos legacy.
     */
    List<Reserva> findByMercadopagoPaymentId(String mercadopagoPaymentId);

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
     * Marca reembolso Mercado Pago aplicado de forma atómica (una fila, solo si aún no estaba marcado
     * y el estado sigue siendo uno que amerita cierre con posible devolución).
     *
     * @return filas actualizadas ({@code 0} o {@code 1})
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Reserva r SET r.mercadopagoReembolsoProcesadoEn = :ahora "
            + "WHERE r.idReserva = :idReserva AND r.mercadopagoReembolsoProcesadoEn IS NULL "
            + "AND r.estadoReserva.idEstadoReserva IN :idsEstadoReembolso")
    int marcarMercadopagoReembolsoProcesadoSiPendiente(
            @Param("idReserva") Integer idReserva,
            @Param("ahora") LocalDateTime ahora,
            @Param("idsEstadoReembolso") Collection<Integer> idsEstadoReembolso);

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

    /**
     * Esperando decisión del paseador: solicitada, pendiente de pago o ya pagada; solicitud más antigua que el umbral.
     */
    @Query("SELECT r.idReserva FROM Reserva r "
            + "WHERE r.estadoReserva.idEstadoReserva IN (:idSolicitada, :idPendientePago, :idPagada) "
            + "AND r.fechaSolicitud < :limite ORDER BY r.idReserva")
    List<Integer> findIdReservasParaExpiracionPorPlazoAceptacion(
            @Param("idSolicitada") Integer idSolicitada,
            @Param("idPendientePago") Integer idPendientePago,
            @Param("idPagada") Integer idPagada,
            @Param("limite") LocalDateTime limite,
            Pageable pageable);

    /**
     * Reconciliación: estados que ameritan devolución MP, con cobro conocido y marca de éxito aún no persistida.
     */
    @Query("SELECT r.idReserva FROM Reserva r "
            + "WHERE r.estadoReserva.idEstadoReserva IN :idsEstado "
            + "AND r.mercadopagoPaymentId IS NOT NULL AND trim(r.mercadopagoPaymentId) <> '' "
            + "AND r.mercadopagoReembolsoProcesadoEn IS NULL "
            + "ORDER BY r.idReserva")
    List<Integer> findIdReservasPendientesReconciliacionReembolsoMercadoPago(
            @Param("idsEstado") Collection<Integer> idsEstado,
            Pageable pageable);

    /**
     * Reembolso MP ya marcado en reserva pero correo tutor aún no confirmado (job de reenvío).
     * Solo estados de cierre con posible devolución ({@code IDS_ESTADO_REEMBOLSO_MERCADOPAGO}), para no reenviar
     * si la fila quedó en un estado incoherente respecto al flujo de reembolso.
     */
    @Query("SELECT r.idReserva FROM Reserva r "
            + "WHERE r.mercadopagoReembolsoProcesadoEn IS NOT NULL AND r.notificacionReembolsoEnviadaEn IS NULL "
            + "AND r.estadoReserva.idEstadoReserva IN :idsEstado "
            + "ORDER BY r.idReserva")
    List<Integer> findIdReservasPendientesNotificacionReembolso(
            @Param("idsEstado") Collection<Integer> idsEstado,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Reserva r SET r.notificacionReembolsoEnviadaEn = :ahora "
            + "WHERE r.idReserva = :idReserva AND r.notificacionReembolsoEnviadaEn IS NULL")
    int marcarNotificacionReembolsoEnviadaSiPendiente(
            @Param("idReserva") Integer idReserva,
            @Param("ahora") LocalDateTime ahora);
}
