package com.patiperro.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaccion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transaccion")
    private Long idTransaccion;

    @Column(name = "id_reserva")
    private Long idReserva;

    @Column(name = "id_pago")
    private Long idPago;

    @Column(name = "monto_bruto", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoBruto;

    @Column(name = "comision_app", nullable = false, precision = 14, scale = 2)
    private BigDecimal comisionApp;

    @Column(name = "monto_neto", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoNeto;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 40)
    private Origen origen;

    @Enumerated(EnumType.STRING)
    @Column(name = "destino", nullable = false, length = 40)
    private Destino destino;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 40)
    private EstadoPago estadoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_transaccion", nullable = false, length = 50)
    private TipoTransaccion tipoTransaccion;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billetera_id")
    private Billetera billetera;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaccion_relacionada_id")
    private Transaccion transaccionRelacionada;

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
