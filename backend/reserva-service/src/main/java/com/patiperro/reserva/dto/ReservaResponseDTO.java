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
    /** Cuando el paseador aceptó (stepper / historial). */
    private LocalDateTime fechaAceptacion;
    private BigDecimal montoTotal;
    /** Id de {@code transaccion} en pagos-service (enlace al cobro). */
    private Long idPago;
    private Integer idEstadoReserva;
    private String nombreEstado;
    private LocalDateTime fechaInicioReal;
    private LocalDateTime fechaFin;
    private Integer codigoEncuentro;
    private LocalDateTime codigoEncuentroExpiraEn;
    private String motivoRechazo;
    private String detalleRechazo;
    /** Copia del bloque de agenda (inicio/fin programados) cuando se enriquece el listado. */
    private LocalDateTime agendaHoraInicio;
    private LocalDateTime agendaHoraFin;
    /** {@code id_usuario} del paseador en agenda_bloque. */
    private Integer idPaseadorUsuario;
    /** {@code true} si el tutor puede iniciar o reintentar checkout (SOLICITADA / PENDIENTE_PAGO). */
    private Boolean puedeReintentarPago;
}
