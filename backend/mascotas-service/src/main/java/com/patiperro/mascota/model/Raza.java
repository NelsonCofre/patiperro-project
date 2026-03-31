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
@Table(name = "raza")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Raza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_raza")
    private Long idRaza;

    @NotBlank(message = "El nombre de la raza es obligatorio")
    @Column(nullable = false, length = 60)
    private String nombre;
}
