package com.patiperro.paseador.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "radio_cobertura")
    private Integer radioCobertura;

    @OneToOne(optional = false)
    @JoinColumn(name = "paseador_id_paseador", nullable = false, unique = true)
    @JsonIgnoreProperties({"configuracion", "contrasena", "fotos"})
    private Paseador paseador;

    @OneToMany(mappedBy = "configuracion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TarifaPaseador> tarifas = new ArrayList<>();
}
