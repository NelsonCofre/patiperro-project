package com.patiperro.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Intento de reserva antes del cobro: el alta en reserva-service ocurre al aprobar el pago (webhook MP).
 */
@Entity
@Table(name = "checkout_reserva_borrador")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutReservaBorrador {

    public static final String ESTADO_PENDING = "PENDING";
    public static final String ESTADO_USED = "USED";
    public static final String ESTADO_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_tutor_usuario", nullable = false)
    private Long idTutorUsuario;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "id_transaccion_pagos")
    private Long idTransaccionPagos;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (estado == null) {
            estado = ESTADO_PENDING;
        }
    }
}
