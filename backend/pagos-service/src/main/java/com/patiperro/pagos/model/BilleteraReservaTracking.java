package com.patiperro.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una fila por reserva con cobro que afecta la billetera del paseador (retenido → verificación → liberado).
 */
@Entity
@Table(
        name = "billetera_reserva_tracking",
        indexes = {
                @Index(name = "idx_billetera_tracking_paseador_liberado", columnList = "id_usuario_paseador, liberado_en"),
                @Index(name = "idx_billetera_tracking_fase_liberado", columnList = "fase, liberado_en")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BilleteraReservaTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tracking")
    private Long idTracking;

    @Column(name = "id_reserva", nullable = false, unique = true)
    private Integer idReserva;

    @Column(name = "id_usuario_paseador", nullable = false)
    private Long idUsuarioPaseador;

    @Column(name = "id_transaccion_pagos", nullable = false)
    private Long idTransaccionPagos;

    @Column(name = "monto_bruto", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoBruto;

    @Column(name = "comision_app", nullable = false, precision = 14, scale = 2)
    private BigDecimal comisionApp;

    @Column(name = "monto_neto", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoNeto;

    @Enumerated(EnumType.STRING)
    @Column(name = "fase", nullable = false, length = 32)
    private BilleteraReservaFase fase;

    /** Momento en que el paseo finalizó (regla N+2 desde el día calendario de esta fecha). */
    @Column(name = "fecha_fin_servicio")
    private LocalDateTime fechaFinServicio;

    /** Cuando el monto pasó a {@link Billetera#getSaldoActual()} (disponible). */
    @Column(name = "liberado_en")
    private LocalDateTime liberadoEn;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
    }
}
