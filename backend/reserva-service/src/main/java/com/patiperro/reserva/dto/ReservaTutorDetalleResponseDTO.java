package com.patiperro.reserva.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservaTutorDetalleResponseDTO {
    private Integer idReserva;
    private Integer idTutorUsuario;
    private Integer idMascota;
    private String mascotaNombre;
    private Integer idAgendaBloque;
    private Integer idPaseador;
    private String paseadorNombre;
    private LocalDate fecha;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFinal;
    private BigDecimal montoTotal;
    /** Id de {@code transaccion} en pagos-service. */
    private Long idPago;
    private Integer idEstadoReserva;
    private String nombreEstado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaAceptacion;
    private LocalDateTime fechaInicioReal;
    private LocalDateTime fechaFin;
    private Integer codigoEncuentro;
    private String tutorNombre;
    private String tutorCorreo;
    /** Fin de validez del PIN (inicio programado + ventana, o ajuste si ya pasó). */
    private LocalDateTime codigoEncuentroExpiraEn;
    private String motivoRechazo;
    private String detalleRechazo;
    /** Último {@code status} MP cuando el cobro no quedó aprobado (webhook). */
    private String mercadopagoUltimoEstado;
    private String mercadopagoUltimoEstadoDetalle;
    private LocalDateTime mercadopagoUltimoEstadoEn;
    /** {@code true} si el tutor puede reintentar el pago (estado SOLICITADA / PENDIENTE_PAGO). */
    private Boolean puedeReintentarPago;
    /** Cuándo pagos-service confirmó reembolso MP (idempotencia); visible vía gateway en listados tutor. */
    private LocalDateTime mercadopagoReembolsoProcesadoEn;
    /** Cuándo se registró envío del correo de reembolso al tutor (puede ser null hasta el job). */
    private LocalDateTime notificacionReembolsoEnviadaEn;
}
