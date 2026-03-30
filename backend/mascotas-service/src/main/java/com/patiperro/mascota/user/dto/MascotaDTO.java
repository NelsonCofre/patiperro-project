package com.patiperro.mascota.user.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class MascotaDTO {
    private Long idMascota;
    private Long idTutor;
    private String nombre;
    private LocalDate fechaNacimiento;
    private String edadCalculada; // Aquí enviaremos "2 años" o "5 meses"
    private Double peso;
    private String tamano;
    private String sexo;
    private String comportamiento;
    private String cuidadosEspeciales;
    private Long idRaza;      // Solo el ID para el formulario
    private String nombreRaza; // El nombre para mostrarlo en el perfil
}