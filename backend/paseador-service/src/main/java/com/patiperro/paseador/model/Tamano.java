package com.patiperro.paseador.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tamano")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tamano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tamano")
    private Long id;

    @Column(name = "nombre", length = 60)
    private String nombre;

    @Column(name = "descripcion", length = 60)
    private String descripcion;
}
