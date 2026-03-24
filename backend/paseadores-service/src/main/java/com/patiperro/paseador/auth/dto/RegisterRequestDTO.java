package com.patiperro.paseador.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RegisterRequestDTO {

    private String rut;
    private String primerNombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private Integer telefono;
    private String correo;
    private String contrasena;

    @NotBlank(message = "fotoPerfil es obligatoria")
    private String fotoPerfil;

    @NotBlank(message = "biografia es obligatoria")
    private String biografia;

    private String pais;
    private String region;
    private String ciudad;
    private String calle;
    private String comuna;
    private Integer numeracion;
    private String casaDepartamento;

    @JsonProperty("fotos")
    private List<String> fotosUrls;
}
