package com.patiperro.mascota.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @Size(min = 2, max = 30, message = "El nombre debe tener entre 2 y 30 caracteres") // El tamaño es entre 2 y 30 incluyendolos //
    @Column(nullable = false, length = 30)
    private String nombre;

    @Column(length = 100)
    private String descripcion;
}
