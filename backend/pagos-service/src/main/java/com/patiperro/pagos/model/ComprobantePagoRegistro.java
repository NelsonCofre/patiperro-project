package com.patiperro.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "comprobante_pago_registro")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComprobantePagoRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "id_reserva", nullable = false, unique = true)
    private Integer idReserva;

    @Column(name = "id_transaccion_pagos")
    private Long idTransaccionPagos;

    @Column(name = "provider_payment_id", length = 64)
    private String providerPaymentId;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
}
