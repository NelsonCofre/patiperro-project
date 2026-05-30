package com.patiperro.notification_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Suscripción Web Push de un dispositivo/navegador ({@code push_suscripcion}).
 * <p>{@code idUsuario} = {@code tutorId} o {@code paseadorId} del JWT al registrar (misma convención que chat).</p>
 * <p>No hay FK a tutores/paseadores: microservicios con BD propia (como otras integraciones del monorepo).</p>
 */
@Entity
@Table(
        name = "push_suscripcion",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_push_suscripcion_endpoint",
                columnNames = "endpoint"),
        indexes = @Index(name = "idx_push_suscripcion_usuario_activa", columnList = "id_usuario, activa"))
@Getter
@Setter
@NoArgsConstructor
public class PushSuscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_suscripcion", nullable = false)
    private Integer idSuscripcion;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "endpoint", nullable = false, columnDefinition = "TEXT")
    private String endpoint;

    @Column(name = "p256dh_key", nullable = false, columnDefinition = "TEXT")
    private String p256dhKey;

    @Column(name = "auth_key", nullable = false, columnDefinition = "TEXT")
    private String authKey;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "activa", nullable = false)
    private boolean activa = true;

    @Column(name = "fecha_alta", nullable = false)
    private Instant fechaAlta = Instant.now();

    @Column(name = "fecha_ultimo_uso")
    private Instant fechaUltimoUso;

    @PrePersist
    void prePersist() {
        if (fechaAlta == null) {
            fechaAlta = Instant.now();
        }
    }
}
