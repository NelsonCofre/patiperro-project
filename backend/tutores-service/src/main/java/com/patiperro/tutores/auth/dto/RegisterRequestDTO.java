package com.patiperro.tutores.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RegisterRequestDTO {

    // Dueño de la mascota (entidad Tutor)
    private String rut;
    private String primerNombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String correo;
    private String contrasena;
    /** Avatar obligatorio; se guarda en tutor.foto_perfil (no en la tabla foto / galeria). */
    @NotBlank(message = "fotoPerfil es obligatoria")
    private String fotoPerfil;
    /** Texto de presentacion; obligatoria en el registro. */
    @NotBlank(message = "biografia es obligatoria")
    private String biografia;

    // Direccion (opcional)
    private String pais;
    private String region;
    private String ciudad;
    private String calle;
    private String comuna;
    private Integer numeracion;
    private String casaDepartamento;

    /** Galeria opcional: URLs extra para tabla foto (distinto de fotoPerfil). JSON: "fotos": [...]. */
    @JsonProperty("fotos")
    private List<String> fotosUrls;
}
