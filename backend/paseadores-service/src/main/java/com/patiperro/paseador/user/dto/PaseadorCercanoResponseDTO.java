package com.patiperro.paseador.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Tarjeta publica de paseador para busqueda por cercania (sin correo ni telefono). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaseadorCercanoResponseDTO {

    private Long idPaseador;
    private String nombreCompleto;
    private String fotoPerfil;
    private String biografia;
    /** Distancia en km desde el punto de referencia del tutor (Haversine). */
    private double distanciaKm;
    private BigDecimal radioCoberturaKm;
    private Double latitud;
    private Double longitud;
}
