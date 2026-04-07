package com.patiperro.reserva.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadoReservaRequestDTO {

    @NotBlank
    @Size(max = 60)
    private String nombreEstado;
}
