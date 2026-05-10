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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "recaudacion_plataforma_log",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_recaudacion_tx_evento", columnNames = {"id_transaccion", "tipo_evento"})
        },
        indexes = {
                @Index(name = "idx_recaudacion_fecha_evento", columnList = "fecha_evento"),
                @Index(name = "idx_recaudacion_tipo_fecha", columnList = "tipo_evento, fecha_evento")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecaudacionPlataformaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log")
    private Long idLog;

    @Column(name = "id_transaccion", nullable = false)
    private Long idTransaccion;

    @Column(name = "id_reserva", nullable = false)
    private Integer idReserva;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 40)
    private TipoEventoRecaudacion tipoEvento;

    @Column(name = "monto_bruto", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoBruto;

    @Column(name = "comision_app", nullable = false, precision = 14, scale = 2)
    private BigDecimal comisionApp;

    @Column(name = "monto_neto", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoNeto;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime fechaEvento;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaEvento == null) {
            fechaEvento = ahora;
        }
        if (creadoEn == null) {
            creadoEn = ahora;
        }
    }
}
