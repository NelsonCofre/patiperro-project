package com.patiperro.paseador.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MiPerfilResponseDTO {

    private Long id;
    private String nombreCompleto;
    private String correo;
    private String telefono;
    private String fotoPerfil;
    private String biografia;
}
