package com.patiperro.paseador.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Resumen público de un paseador (sin datos de verificación de identidad sensibles). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaseadorResumenResponseDTO {
    private Long idPaseador;
    private String nombreCompleto;
    private String fotoPerfil;
    /** Correo de contacto del paseador (para notificaciones servidor-servidor). */
    private String correo;
    /** true si la verificación de identidad (cédula) fue aprobada (JSON: {@code esVerificado}). */
    @JsonAlias("verificado")
    private boolean esVerificado;
}
