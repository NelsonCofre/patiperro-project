package com.patiperro.resena.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TutorDTO {
    private Long id;

    // Mapeamos 'primerNombre' del JSON a nuestra variable 'nombre'
    @JsonProperty("primerNombre")
    private String nombre;

    // Mapeamos 'apellidoPaterno' del JSON a nuestra variable 'apellido'
    @JsonProperty("apellidoPaterno")
    private String apellido;

    // Mapeamos 'fotoPerfil' del JSON a nuestra variable 'fotoUrl'
    @JsonProperty("fotoPerfil")
    private String fotoUrl;
}