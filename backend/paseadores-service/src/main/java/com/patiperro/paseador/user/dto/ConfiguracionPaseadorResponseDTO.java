package com.patiperro.paseador.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ConfiguracionPaseadorResponseDTO {
    private Long configuracionId;
    private BigDecimal radioCoberturaKm;
    private List<TarifaConfiguracionResponseDTO> tarifas;
}
