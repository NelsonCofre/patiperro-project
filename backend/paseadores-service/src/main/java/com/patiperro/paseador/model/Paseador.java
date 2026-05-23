package com.patiperro.paseador.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "paseador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paseador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paseador")
    private Long id;

    @Column(name = "rut", length = 20)
    private String rut;

    @Column(name = "primer_nombre", length = 60)
    private String primerNombre;

    @Column(name = "segundo_nombre", length = 60)
    private String segundoNombre;

    @Column(name = "apellido_paterno", length = 60)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", length = 60)
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    /** En el modelo ER viene como INTEGER; si necesitas +56 usa String en una migracion futura. */
    @Column(name = "telefono")
    private Integer telefono;

    @Column(name = "correo", length = 60, unique = true, nullable = false)
    private String correo;

    @Column(name = "foto_perfil", length = 150)
    private String fotoPerfil;

    @Column(name = "biografia", length = 250)
    private String biografia;

    // Verificacion de identidad (cedula). Distinto de saldo_verificacion en pagos-service.
    // No se serializa a JSON; el cliente usa VerificacionIdentidadResponseDTO.
    @JsonIgnore
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_verificacion_identidad", length = 20, nullable = false)
    @Builder.Default
    private EstadoVerificacionIdentidad estadoVerificacionIdentidad = EstadoVerificacionIdentidad.SIN_ENVIAR;

    // Nombre de archivo en disco (UUID.ext), no ruta publica.
    @JsonIgnore
    @Column(name = "archivo_cedula_frontal", length = 255)
    private String archivoCedulaFrontal;

    @JsonIgnore
    @Column(name = "archivo_cedula_reverso", length = 255)
    private String archivoCedulaReverso;

    @JsonIgnore
    @Column(name = "verificacion_identidad_enviada_en")
    private LocalDateTime verificacionIdentidadEnviadaEn;

    @JsonIgnore
    @Column(name = "verificacion_identidad_revisada_en")
    private LocalDateTime verificacionIdentidadRevisadaEn;

    @JsonIgnore
    @Column(name = "motivo_rechazo_verificacion_identidad", length = 500)
    private String motivoRechazoVerificacionIdentidad;

    // Contrasena hash; nunca en respuestas JSON.
    @JsonIgnore
    @Column(name = "contrasena", length = 60, nullable = false)
    private String contrasena;

    /** 1:1 con direccion: FK {@code direccion_id_direccion} en tabla {@code paseador} (igual que tutor). */
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "direccion_id_direccion", nullable = true)
    private Direccion direccion;

    @JsonIgnore
    @OneToOne(mappedBy = "paseador", cascade = CascadeType.ALL, orphanRemoval = true)
    private Configuracion configuracion;

    @JsonIgnore
    @OneToMany(mappedBy = "paseador", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Foto> fotos = new ArrayList<>();

    @PrePersist
    @PreUpdate
    private void normalizarVerificacionIdentidad() {
        if (estadoVerificacionIdentidad == null) {
            estadoVerificacionIdentidad = EstadoVerificacionIdentidad.SIN_ENVIAR;
        }
    }
}
