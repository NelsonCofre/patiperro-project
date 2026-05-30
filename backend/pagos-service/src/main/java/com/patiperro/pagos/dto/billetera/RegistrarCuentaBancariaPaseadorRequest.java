package com.patiperro.pagos.dto.billetera;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta o actualización de la cuenta bancaria asociada a la billetera del paseador (una por billetera).
 */
public record RegistrarCuentaBancariaPaseadorRequest(
        @NotNull Long bancoId,
        @NotNull Long tipoCuentaId,
        @NotBlank @Size(min = 4, max = 60) String numeroCuenta) {
}
