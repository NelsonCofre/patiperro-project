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
@Table(name = "tamano")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tamano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tamano")
    private Long idTamano;

    @NotBlank(message = "El nombre del tamaño es obligatorio")
    @Column(nullable = false, length = 30)
    private String nombre;

    @Column(length = 100)
    private String descripcion;
}
