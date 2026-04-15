package com.patiperro.reserva.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class BookingStatusPatchRequestDTO {

    private PaseadorDecision decision;

    @Positive
    private Integer idEstadoReserva;

    @AssertTrue(message = "Debe enviar decision o idEstadoReserva (solo uno)")
    public boolean isCuerpoValido() {
        return (decision == null) ^ (idEstadoReserva == null);
    }
}
