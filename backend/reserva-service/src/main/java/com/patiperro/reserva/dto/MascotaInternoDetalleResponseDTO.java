package com.patiperro.reserva.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MascotaInternoDetalleResponseDTO {
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

