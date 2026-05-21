package com.patiperro.paseador.user.dto;

import com.patiperro.paseador.model.EstadoVerificacionIdentidad;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificacionIdentidadResponseDTO {

    private EstadoVerificacionIdentidad estado;
    private String estadoEtiqueta;
    private boolean puedeSubir;
    private LocalDateTime enviadoEn;
    private LocalDateTime revisadoEn;
    private String motivoRechazo;
    private boolean tieneFrontal;
    private boolean tieneReverso;
}
