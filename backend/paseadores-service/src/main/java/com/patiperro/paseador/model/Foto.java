package com.patiperro.paseador.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

/**
 * Galeria del paseador. La columna FK conserva el nombre {@code tutor_id_tutor} del modelo entregado.
 */
@Entity
@Table(name = "foto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Foto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_foto")
    private Long id;

    @Column(name = "url", length = 150)
    private String url;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id_tutor", nullable = false)
    @JsonIgnoreProperties({"fotos", "contrasena", "configuracion"})
    private Paseador paseador;
}
