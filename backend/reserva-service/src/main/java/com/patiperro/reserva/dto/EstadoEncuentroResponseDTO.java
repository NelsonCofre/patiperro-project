package com.patiperro.reserva.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadoEncuentroResponseDTO {
    private Integer idReserva;
    private String estadoEncuentro; // PENDIENTE | CONFIRMADO | FALLIDO
    private Integer idEstadoReserva;
    private String nombreEstadoReserva;
    private Integer intentosFallidos;
    private LocalDateTime bloqueadoHasta;
    private Long puedeReintentarEnSegundos;
    private String mensaje;
}

