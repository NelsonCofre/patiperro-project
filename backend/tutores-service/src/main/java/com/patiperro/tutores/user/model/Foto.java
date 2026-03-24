package com.patiperro.tutores.user.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

/** Una fila por imagen de galeria del perfil del dueño (tutor); no es el avatar foto_perfil. */
@Entity
@Table(name = "foto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Foto {

    // PK de foto.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_foto")
    private Long id;

    // URL publica o interna de la foto.
    @Column(name = "url", length = 150)
    private String url;

    // Relacion N:1 -> muchas fotos pertenecen a un tutor.
    // JsonIgnoreProperties evita ciclos de serializacion al exponer foto + tutor.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id_usuario", nullable = false)
    @JsonIgnoreProperties({"fotos", "contrasena"})
    private Tutor tutor;
}
