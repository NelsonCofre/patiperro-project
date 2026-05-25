package com.patiperro.paseador.dto.user;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    /** true si la verificación de identidad fue aprobada (JSON: {@code esVerificado}). */
    @JsonAlias("verificado")
    private boolean esVerificado;
}