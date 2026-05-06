package com.patiperro.pagos.dto.billetera;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SolicitarRetiroPaseadorRequest(
        @NotNull
        @DecimalMin(value = "0.01", inclusive = true)
        @Digits(integer = 12, fraction = 2)
        BigDecimal monto
) {
}
