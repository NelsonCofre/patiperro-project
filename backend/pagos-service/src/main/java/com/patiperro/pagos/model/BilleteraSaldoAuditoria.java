package com.patiperro.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(
        name = "billetera_saldo_auditoria",
        indexes = {
                @Index(name = "idx_billetera_saldo_aud_usuario_creado", columnList = "id_usuario_paseador, creado_en"),
                @Index(name = "idx_billetera_saldo_aud_tx", columnList = "id_transaccion")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BilleteraSaldoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @Column(name = "id_tracking", nullable = false, unique = true)
    private Long idTracking;

    @Column(name = "id_reserva", nullable = false)
    private Integer idReserva;

    @Column(name = "id_usuario_paseador", nullable = false)
    private Long idUsuarioPaseador;

    /** Cobro del tutor ({@code BilleteraReservaTracking#idTransaccionPagos}), opción A. */
    @Column(name = "id_transaccion", nullable = false)
    private Long idTransaccion;

    @Column(name = "monto_neto", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoNeto;

    @Column(name = "saldo_verificacion_antes", nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoVerificacionAntes;

    @Column(name = "saldo_actual_antes", nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoActualAntes;

    @Column(name = "saldo_verificacion_despues", nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoVerificacionDespues;

    @Column(name = "saldo_actual_despues", nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoActualDespues;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
    }
}
