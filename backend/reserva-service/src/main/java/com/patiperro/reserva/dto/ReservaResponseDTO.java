package com.patiperro.reserva.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservaResponseDTO {

    private Integer idReserva;
    private Integer idTutorUsuario;
    private Integer idMascota;
    private Integer idAgendaBloque;
    private Integer idTarifa;
    private LocalDateTime fechaSolicitud;
    private BigDecimal montoTotal;
    private Integer idPago;
    private Integer idEstadoReserva;
    private String nombreEstado;
    private LocalDateTime fechaInicioReal;
    private LocalDateTime fechaFin;
    private Integer codigoEncuentro;
}
