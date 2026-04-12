package com.patiperro.paseador.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "direccion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_direccion")
    private Long id;

    @Column(name = "pais", length = 60)
    private String pais;

    @Column(name = "region", length = 60)
    private String region;

    @Column(name = "ciudad", length = 60)
    private String ciudad;

    @Column(name = "calle", length = 60)
    private String calle;

    @Column(name = "comuna", length = 60)
    private String comuna;

    @Column(name = "numeracion")
    private Integer numeracion;

    @Column(name = "casa_departamento", length = 60)
    private String casaDepartamento;

    /** WGS84; opcional (geocodificacion / mapas). */
    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;
}
