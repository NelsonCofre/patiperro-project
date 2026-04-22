package com.patiperro.reserva.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CodigoReservaValidarRequestDTO {

    @NotNull
    @Positive
    private Integer idReserva;

    /** 4 dígitos ingresados por el paseador. */
    @NotNull
    @Pattern(regexp = "^\\d{4}$", message = "codigoIngresado debe contener exactamente 4 dígitos")
    private String codigoIngresado;
}

