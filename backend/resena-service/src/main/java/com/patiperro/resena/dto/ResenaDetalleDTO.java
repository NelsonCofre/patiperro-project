package com.patiperro.resena.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResenaDetalleDTO {
    private Long id;
    private Integer estrellas;
    private String comentario;
    private String nombreTutor; // "Nombre + Apellido"
    private Integer idReserva;
}