package com.patiperro.mascota.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "especie")
@Data
@NoArgsConstructor
public class Especie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especie")
    private Long idEspecie;

    @NotBlank(message = "El nombre de la especie es obligatorio")
    @Column(nullable = false, length = 60)
    private String nombre;

    /** Lado inverso: una especie tiene muchas {@link Raza}. No se serializa en JSON para evitar ciclos y cargas pesadas. */
    @OneToMany(mappedBy = "especie")
    @JsonIgnore
    private List<Raza> razas = new ArrayList<>();
}
