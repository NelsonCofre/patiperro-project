package com.patiperro.resena.dto;

import lombok.Data;

@Data
public class ReservaDTO {
    private Integer id;
    private String nombreEstado; // Vital para validar que esté "FINALIZADA"
}