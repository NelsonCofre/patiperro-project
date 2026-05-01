package com.patiperro.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pago_externo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoExterno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago_externo")
    private Long idPagoExterno;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaccion_id", nullable = false, unique = true)
    private Transaccion transaccion;

    @Column(name = "provider", nullable = false, length = 60)
    private String provider;

    @Column(name = "provider_payment_id", length = 120)
    private String providerPaymentId;

    @Column(name = "preference_id", length = 120)
    private String preferenceId;

    @Column(name = "external_reference", length = 120)
    private String externalReference;

    @Column(name = "status", length = 60)
    private String status;

    @Column(name = "status_detail", length = 150)
    private String statusDetail;

    @Column(name = "medio_pago", length = 100)
    private String medioPago;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @Lob
    @Column(name = "response_json")
    private String responseJson;

    /** Id del reembolso en Mercado Pago ({@code POST .../refunds}). */
    @Column(name = "refund_provider_id", length = 64)
    private String refundProviderId;

    @Column(name = "refund_status", length = 60)
    private String refundStatus;

    @Column(name = "refund_fecha")
    private LocalDateTime refundFecha;

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
