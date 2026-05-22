package com.patiperro.paseador.user.dto;

import com.patiperro.paseador.model.EstadoVerificacionIdentidad;
import com.patiperro.paseador.model.Paseador;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Estado del flujo de verificación de identidad para el paseador autenticado
 * ({@code GET/POST /api/paseadores/me/verificacion}).
 * <p>
 * No incluye rutas de archivo ni URLs públicas; la descarga va por endpoint autenticado.
 * Distinto del bucket {@code verificacion} de billetera en pagos-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificacionIdentidadResponseDTO {

    private EstadoVerificacionIdentidad estado;
    /** Texto listo para UI; derivado de {@link EstadoVerificacionIdentidad#getEtiqueta()}. */
    private String estadoEtiqueta;
    /** false si el paseador debe esperar revisión admin o ya está aprobado. */
    private boolean puedeSubir;
    private LocalDateTime enviadoEn;
    private LocalDateTime revisadoEn;
    /** Solo se envía si {@code estado == RECHAZADO}. */
    private String motivoRechazo;
    /** Indica si hay documento guardado; no expone el nombre interno del archivo. */
    private boolean tieneFrontal;
    private boolean tieneReverso;

    public static VerificacionIdentidadResponseDTO from(Paseador paseador) {
        if (paseador == null) {
            throw new IllegalArgumentException("Paseador requerido");
        }
        EstadoVerificacionIdentidad estado = paseador.getEstadoVerificacionIdentidad() != null
                ? paseador.getEstadoVerificacionIdentidad()
                : EstadoVerificacionIdentidad.SIN_ENVIAR;
        return VerificacionIdentidadResponseDTO.builder()
                .estado(estado)
                .estadoEtiqueta(estado.getEtiqueta())
                .puedeSubir(estado.puedeSubirDocumentos())
                .enviadoEn(paseador.getVerificacionIdentidadEnviadaEn())
                .revisadoEn(paseador.getVerificacionIdentidadRevisadaEn())
                .motivoRechazo(estado == EstadoVerificacionIdentidad.RECHAZADO
                        ? paseador.getMotivoRechazoVerificacionIdentidad()
                        : null)
                .tieneFrontal(tieneArchivo(paseador.getArchivoCedulaFrontal()))
                .tieneReverso(tieneArchivo(paseador.getArchivoCedulaReverso()))
                .build();
    }

    private static boolean tieneArchivo(String filename) {
        return filename != null && !filename.isBlank();
    }
}
