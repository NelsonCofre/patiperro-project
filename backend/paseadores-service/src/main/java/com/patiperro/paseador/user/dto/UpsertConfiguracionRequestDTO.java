package com.patiperro.paseador.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpsertConfiguracionRequestDTO {

    @NotNull(message = "radioCoberturaKm es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "radioCoberturaKm no puede ser negativo")
    private BigDecimal radioCoberturaKm;

    @NotEmpty(message = "Debe enviar al menos una tarifa")
    @Valid
    private List<TarifaInputDTO> tarifas;
}
