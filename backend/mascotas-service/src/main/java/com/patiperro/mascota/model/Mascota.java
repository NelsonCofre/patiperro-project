package com.patiperro.mascota.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;
import java.time.LocalDate;


@Entity
@Table(name = "mascota")
@Data // Esto genera automáticamente Getters y Setters con Lombok
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMascota;

    @NotNull(message = "El ID del tutor es obligatorio")
    private Long idTutor; // Relación lógica con el microservicio de Usuarios

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 60)
    private String nombre;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
    private LocalDate fechaNacimiento;

    @NotNull(message = "El peso es obligatorio")
    @DecimalMin(value = "0.1", message = "El peso debe ser un número válido mayor a 0 kg")
    private Double peso;

    @Column(length = 60)
    private String tamano; // Pequeño, Mediano, Grande

    @Column(length = 10)
    private String sexo;

    @Column(columnDefinition = "TEXT")
    private String comportamiento;

    @Column(columnDefinition = "TEXT")
    private String cuidadosEspeciales;

    // Relación con la tabla Raza para el dropdown estandarizado
    @ManyToOne
    @JoinColumn(name = "id_raza")
    private Raza raza;

    
}