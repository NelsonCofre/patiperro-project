package com.patiperro.notification_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Suscripción Web Push de un dispositivo/navegador.
 * {@code idUsuario} = claim {@code tutorId} o {@code paseadorId} del JWT (misma convención que chat/reserva).
 */
@Entity
@Table(
        name = "push_suscripcion",
        indexes = @Index(name = "idx_push_suscripcion_usuario_activa", columnList = "id_usuario, activa"))
@Getter
@Setter
@NoArgsConstructor
public class PushSuscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_suscripcion")
    private Integer idSuscripcion;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String endpoint;

    @Column(name = "p256dh_key", nullable = false, columnDefinition = "TEXT")
    private String p256dhKey;

    @Column(name = "auth_key", nullable = false, columnDefinition = "TEXT")
    private String authKey;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "fecha_alta", nullable = false)
    private Instant fechaAlta;

    @Column(name = "fecha_ultimo_uso")
    private Instant fechaUltimoUso;

    @PrePersist
    void prePersist() {
        if (fechaAlta == null) {
            fechaAlta = Instant.now();
        }
    }
}
