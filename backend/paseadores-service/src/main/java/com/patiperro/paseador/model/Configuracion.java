package com.patiperro.paseador.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "configuracion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Configuracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracion")
    private Long id;

    /** Kilómetros de cobertura; admite decimales, p.ej. 0.2 km. */
    @Column(name = "radio_cobertura", precision = 8, scale = 2)
    private BigDecimal radioCobertura;

    @OneToOne(optional = false)
    @JoinColumn(name = "paseador_id_paseador", nullable = false, unique = true)
    @JsonIgnoreProperties({"configuracion", "contrasena", "fotos"})
    private Paseador paseador;

    @OneToMany(mappedBy = "configuracion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TarifaPaseador> tarifas = new ArrayList<>();
}
