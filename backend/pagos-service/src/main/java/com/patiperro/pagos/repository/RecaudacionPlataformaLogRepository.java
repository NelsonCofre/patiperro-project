package com.patiperro.pagos.repository;

import com.patiperro.pagos.model.RecaudacionPlataformaLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface RecaudacionPlataformaLogRepository extends JpaRepository<RecaudacionPlataformaLog, Long> {

    /**
     * SQL nativo PostgreSQL: requiere uk_recaudacion_tx_evento para que ON CONFLICT preserve idempotencia.
     */
    @Modifying
    @Query(
            value = """
                    INSERT INTO recaudacion_plataforma_log
                        (id_transaccion, id_reserva, tipo_evento, monto_bruto, comision_app, monto_neto, fecha_evento)
                    VALUES
                        (:idTransaccion, :idReserva, :tipoEvento, :montoBruto, :comisionApp, :montoNeto, :fechaEvento)
                    ON CONFLICT (id_transaccion, tipo_evento) DO NOTHING
                    """,
            nativeQuery = true)
    int insertarLog(
            @Param("idTransaccion") Long idTransaccion,
            @Param("idReserva") Integer idReserva,
            @Param("tipoEvento") String tipoEvento,
            @Param("montoBruto") BigDecimal montoBruto,
            @Param("comisionApp") BigDecimal comisionApp,
            @Param("montoNeto") BigDecimal montoNeto,
            @Param("fechaEvento") LocalDateTime fechaEvento);

    /**
     * SQL nativo PostgreSQL: date_trunc agrupa por day/month. El rango es semiabierto: [desde, hasta).
     */
    @Query(
            value = """
                    SELECT date_trunc(:periodo, fecha_evento) AS periodo,
                           COALESCE(SUM(comision_app), 0) AS "totalComision",
                           COUNT(id_log) AS "totalEventos"
                    FROM recaudacion_plataforma_log
                    WHERE fecha_evento >= :desde
                      AND fecha_evento < :hasta
                    GROUP BY date_trunc(:periodo, fecha_evento)
                    ORDER BY periodo
                    """,
            nativeQuery = true)
    List<RecaudacionPeriodoProjection> acumularPorPeriodo(
            @Param("periodo") String periodo,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    interface RecaudacionPeriodoProjection {
        LocalDateTime getPeriodo();

        BigDecimal getTotalComision();

        Long getTotalEventos();
    }
}
