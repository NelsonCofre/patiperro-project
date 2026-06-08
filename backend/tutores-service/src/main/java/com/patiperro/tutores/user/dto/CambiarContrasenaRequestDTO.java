package com.patiperro.tutores.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CambiarContrasenaRequestDTO {

    @NotBlank(message = "La contraseña actual es obligatoria")
    private String contrasenaActual;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    private String contrasenaNueva;
}
