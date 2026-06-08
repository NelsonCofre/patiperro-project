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
    /** {@code true} si el tutor puede reintentar el pago (estado SOLICITADA / PENDIENTE_PAGO). */
    private Boolean puedeReintentarPago;
    /** Comuna del tutor (punto de encuentro del paseo). */
    private String comuna;
    /** Calle, número y referencia del tutor para el encuentro. */
    private String direccionReferencia;
    /** Ruta relativa o URL de foto de perfil del paseador asignado. */
    private String paseadorFotoPerfil;
}
