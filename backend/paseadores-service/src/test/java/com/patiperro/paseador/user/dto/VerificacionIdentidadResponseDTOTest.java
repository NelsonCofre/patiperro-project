package com.patiperro.paseador.user.dto;

import com.patiperro.paseador.model.EstadoVerificacionIdentidad;
import com.patiperro.paseador.model.Paseador;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificacionIdentidadResponseDTOTest {

    @Test
    void from_enProceso_bloqueaSubidaSinMotivoRechazo() {
        Paseador p = Paseador.builder()
                .estadoVerificacionIdentidad(EstadoVerificacionIdentidad.EN_PROCESO)
                .archivoCedulaFrontal("a.jpg")
                .archivoCedulaReverso("b.jpg")
                .motivoRechazoVerificacionIdentidad("motivo viejo")
                .build();

        VerificacionIdentidadResponseDTO dto = VerificacionIdentidadResponseDTO.from(p);

        assertEquals(EstadoVerificacionIdentidad.EN_PROCESO, dto.getEstado());
        assertEquals("Verificación en proceso", dto.getEstadoEtiqueta());
        assertFalse(dto.isPuedeSubir());
        assertTrue(dto.isTieneFrontal());
        assertTrue(dto.isTieneReverso());
        assertNull(dto.getMotivoRechazo());
    }

    @Test
    void from_rechazado_exponeMotivoYPuedeSubir() {
        Paseador p = Paseador.builder()
                .estadoVerificacionIdentidad(EstadoVerificacionIdentidad.RECHAZADO)
                .motivoRechazoVerificacionIdentidad("Documento ilegible")
                .build();

        VerificacionIdentidadResponseDTO dto = VerificacionIdentidadResponseDTO.from(p);

        assertTrue(dto.isPuedeSubir());
        assertEquals("Documento ilegible", dto.getMotivoRechazo());
    }

    @Test
    void from_aprobado_sinMotivoRechazo() {
        Paseador p = Paseador.builder()
                .estadoVerificacionIdentidad(EstadoVerificacionIdentidad.APROBADO)
                .motivoRechazoVerificacionIdentidad("no deberia salir")
                .build();

        VerificacionIdentidadResponseDTO dto = VerificacionIdentidadResponseDTO.from(p);

        assertFalse(dto.isPuedeSubir());
        assertNull(dto.getMotivoRechazo());
    }
}
