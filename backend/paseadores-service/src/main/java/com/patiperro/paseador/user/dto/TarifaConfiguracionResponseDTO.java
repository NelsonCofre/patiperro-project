package com.patiperro.paseador.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TarifaConfiguracionResponseDTO {
    private Long tamanoId;
    private String tamanoNombre;
    private Integer precioPorHora;
}
