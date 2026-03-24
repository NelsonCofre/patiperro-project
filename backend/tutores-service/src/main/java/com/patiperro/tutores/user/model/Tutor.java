package com.patiperro.tutores.user.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Usuario negocio: dueño de la mascota (cliente que registra perros y contrata paseos).
 * En BD y API se mantiene el nombre {@code tutor} por compatibilidad.
 */
@Entity
@Table(name = "tutor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tutor {

    // PK de tutor.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tutor")
    private Long id;

    // Identificador nacional (formato chileno esperado).
    @Column(name = "rut", length = 10)
    private String rut;

    // Primer nombre del tutor.
    @Column(name = "primer_nombre", length = 60)
    private String primerNombre;

    // Segundo nombre del tutor.
    @Column(name = "segundo_nombre", length = 60)
    private String segundoNombre;

    // Apellido paterno del tutor.
    @Column(name = "apellido_paterno", length = 60)
    private String apellidoPaterno;

    // Apellido materno del tutor.
    @Column(name = "apellido_materno", length = 60)
    private String apellidoMaterno;

    // Fecha de nacimiento del tutor.
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    // Telefono de contacto.
    @Column(name = "telefono")
    private String telefono;

    // Correo unico usado para login.
    @Column(name = "correo", length = 60, unique = true, nullable = false)
    private String correo;

    // Avatar / imagen principal del perfil (una sola URL en columna foto_perfil).
    @Column(name = "foto_perfil", length = 150)
    private String fotoPerfil;

    // Texto breve de presentacion.
    @Column(name = "biografia", length = 250)
    private String biografia;

    // Contrasena de acceso.
    // Se ignora en respuestas JSON para no exponerla.
    @JsonIgnore
    @Column(name = "contrasena", length = 60, nullable = false)
    private String contrasena;

    // Relacion 1:1 con Direccion.
    // Cascade.ALL permite persistir/actualizar la direccion junto al tutor.
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "direccion_id_direccion", nullable = true)
    private Direccion direccion;

    // Galeria 1:N (tabla foto); distinto de fotoPerfil.
    @JsonIgnore
    @OneToMany(mappedBy = "tutor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Foto> fotos = new ArrayList<>();
}
