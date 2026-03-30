package com.patiperro.mascota.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "raza")
@Data // Genera Getters, Setters, toString, etc.
@NoArgsConstructor // Constructor vacío (necesario para JPA)
@AllArgsConstructor // Constructor con todos los campos (útil para pruebas)
public class Raza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRaza;

    @NotBlank(message = "El nombre de la raza es obligatorio")
    @Column(nullable = false, length = 60, unique = true)
    private String nombre;

}