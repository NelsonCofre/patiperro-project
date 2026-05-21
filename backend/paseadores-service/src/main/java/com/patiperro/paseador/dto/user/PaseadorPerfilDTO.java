package com.patiperro.paseador.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaseadorPerfilDTO {
    private Long idUsuario;
    private String nombre;
    private String correo; // ¡El campo vital que necesitamos para Brevo!
    /** true si la verificación de identidad fue aprobada por un administrador. */
    private boolean verificado;
}