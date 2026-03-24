package com.patiperro.tutores.user.model;

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

    // PK de direccion.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_direccion")
    private Long id;

    // Pais de residencia.
    @Column(name = "pais", length = 60)
    private String pais;

    // Region/estado.
    @Column(name = "region", length = 60)
    private String region;

    // Ciudad.
    @Column(name = "ciudad", length = 60)
    private String ciudad;

    // Calle.
    @Column(name = "calle", length = 60)
    private String calle;

    // Comuna.
    @Column(name = "comuna", length = 60)
    private String comuna;

    // Numero de calle.
    @Column(name = "numeracion")
    private Integer numeracion;

    // Complemento: casa/departamento.
    @Column(name = "casa_departamento", length = 60)
    private String casaDepartamento;
}
