package com.patiperro.paseador.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TarifaInputDTO {

    @NotNull(message = "tamanoId es obligatorio")
    private Long tamanoId;

    @NotNull(message = "precioPorHora es obligatorio")
    @Min(value = 1, message = "precioPorHora debe ser mayor a 0")
    private Integer precioPorHora;
}
