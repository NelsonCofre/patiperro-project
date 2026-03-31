package com.patiperro.mascota.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "especie")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Especie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especie")
    private Long idEspecie;

    @NotBlank(message = "El nombre de la especie es obligatorio")
    @Column(nullable = false, length = 60)
    private String nombre;
}
