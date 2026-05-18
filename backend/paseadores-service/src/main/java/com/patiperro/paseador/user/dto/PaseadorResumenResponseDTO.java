package com.patiperro.paseador.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaseadorResumenResponseDTO {
    private Long idPaseador;
    private String nombreCompleto;
    private String fotoPerfil;
    /** Correo de contacto del paseador (para notificaciones servidor-servidor). */
    private String correo;
}
