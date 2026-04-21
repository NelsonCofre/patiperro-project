package com.patiperro.reserva.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodigoReservaValidarResponseDTO {
    private Integer idReserva;
    private boolean valido;
    private Integer idEstadoReserva;
    private String nombreEstado;
    private LocalDateTime fechaInicioReal;
}

