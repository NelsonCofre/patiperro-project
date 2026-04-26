package com.patiperro.reserva.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EncuentroConfirmadoEventDTO {
    private String tipo; // ENCUENTRO_CONFIRMADO
    private Integer idReserva;
    private Integer idTutorUsuario;
    private Integer idPaseadorUsuario;
    private String mensajeTutor;
    private String mensajePaseador;
    private String mascotaNombre;
    private String direccionInicio;
    private LocalDateTime horaInicioRegistrada;
    private boolean trackingActivo;
    private boolean chatActivo;
}

