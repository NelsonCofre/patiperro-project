package com.patiperro.reserva.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservaRequestDTO {

    @NotNull
    @Positive
    private Integer idTutorUsuario;

    @NotNull
    @Positive
    private Integer idMascota;

    @NotNull
    @Positive
    private Integer idAgendaBloque;

    @NotNull
    @Positive
    private Integer idTarifa;

    @NotNull
    private LocalDateTime fechaSolicitud;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal montoTotal;

    private Integer idPago;

    @NotNull
    @Positive
    private Integer idEstadoReserva;

    private LocalDateTime fechaInicioReal;

    private LocalDateTime fechaFin;

    private Integer codigoEncuentro;
}
