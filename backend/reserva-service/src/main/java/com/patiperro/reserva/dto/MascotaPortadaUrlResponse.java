package com.patiperro.reserva.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MascotaPortadaUrlResponse {
    private String url;
    /** Nombre de la mascota (opcional). */
    private String nombre;
}
