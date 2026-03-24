package com.patiperro.paseador.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tarifa_paseador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarifaPaseador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tarifa")
    private Long id;

    @Column(name = "precio_base")
    private Integer precioBase;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "configuracion_id_configuracion", nullable = false)
    @JsonIgnoreProperties({"tarifas", "paseador"})
    private Configuracion configuracion;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tamano_id_tamano", nullable = false)
    private Tamano tamano;
}
