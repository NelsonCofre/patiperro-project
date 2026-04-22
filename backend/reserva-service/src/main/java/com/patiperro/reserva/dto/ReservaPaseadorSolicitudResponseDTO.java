package com.patiperro.reserva.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Solicitud de reserva pendiente para el panel del paseador (aceptar / rechazar).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservaPaseadorSolicitudResponseDTO {

    private Integer idReserva;
    private Integer idTutorUsuario;
    private Integer idMascota;
    private Integer idAgendaBloque;
    private BigDecimal montoTotal;
    private LocalDateTime fechaSolicitud;
    private String nombreEstado;

    /** Fecha del bloque de agenda (solo fecha). */
    private String fechaAgenda;
    /** Hora inicio del bloque, formato HH:mm (Chile / local del servicio agenda). */
    private String horaInicio;
    /** Hora fin del bloque, formato HH:mm. */
    private String horaFin;

    private String comuna;
    private String direccionReferencia;

    private String tutorNombre;
    private String tutorTelefono;
    private String tutorCorreo;
    private String tutorFotoUrl;
    private String tutorNotas;

    /** URL de portada ({@code foto_perfil} o primera galería); vacío si no hay. */
    private String mascotaFotoUrl;

    /** Nombre legible; si no hay integración con mascotas, puede ser "Mascota #id". */
    private String mascotaNombre;
    private Integer codigoEncuentro;
    private String mascotaRaza;
    private String mascotaTamano;
    private String mascotaEdad;
    private String mascotaPeso;
    private String mascotaSexo;
    private String mascotaCaracter;
    private String mascotaCuidados;
}
