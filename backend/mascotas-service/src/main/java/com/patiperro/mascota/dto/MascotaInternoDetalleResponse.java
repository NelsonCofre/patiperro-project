package com.patiperro.mascota.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resumen interno para pantallas del paseador (reserva-service).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MascotaInternoDetalleResponse {
    private Long idMascota;
    private String nombre;
    private String fotoPerfil;
    private String raza;
    private String tamano;
    private String edad;
    private String peso;
    private String sexo;
    private String caracter;
    private String cuidados;
}

