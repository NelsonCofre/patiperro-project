package com.patiperro.reserva.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodigoReservaActivoResponseDTO {
    private Integer idReserva;
    /** PIN de 4 dígitos listo para mostrar. */
    private String codigo;
    /** Instante de expiración del PIN. */
    private LocalDateTime expiraEn;
    /** Segundos restantes para contador regresivo. */
    private Long segundosRestantes;
    /** Indica si el PIN fue regenerado por expiración en la consulta. */
    private boolean regenerado;
}

