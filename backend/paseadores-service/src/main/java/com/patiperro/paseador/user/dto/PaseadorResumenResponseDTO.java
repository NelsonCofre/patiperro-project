package com.patiperro.paseador.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaseadorResumenResponseDTO {
    private Long idPaseador;
    private String nombreCompleto;
    private String fotoPerfil;
}
