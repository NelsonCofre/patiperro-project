package com.patiperro.paseador.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaseadorVerificacionCamposTest {

    @Test
    void builder_defaultEstadoSinEnviarYArchivosNull() {
        Paseador p = Paseador.builder()
                .correo("nuevo@test.cl")
                .contrasena("hash")
                .build();

        assertEquals(EstadoVerificacionIdentidad.SIN_ENVIAR, p.getEstadoVerificacionIdentidad());
        assertFalse(p.isEsVerificado());
        assertNull(p.getArchivoCedulaFrontal());
        assertNull(p.getArchivoCedulaReverso());
        assertNull(p.getVerificacionIdentidadEnviadaEn());
        assertNull(p.getVerificacionIdentidadRevisadaEn());
        assertNull(p.getMotivoRechazoVerificacionIdentidad());
    }

    @Test
    void builder_aceptaCamposVerificacionAlineadosConTablaPaseador() {
        LocalDateTime enviado = LocalDateTime.of(2026, 5, 22, 10, 0);
        LocalDateTime revisado = LocalDateTime.of(2026, 5, 23, 15, 30);

        Paseador p = Paseador.builder()
                .correo("verificado@test.cl")
                .contrasena("hash")
                .estadoVerificacionIdentidad(EstadoVerificacionIdentidad.EN_PROCESO)
                .archivoCedulaFrontal("a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg")
                .archivoCedulaReverso("b2c3d4e5-f6a7-8901-bcde-f12345678901.png")
                .verificacionIdentidadEnviadaEn(enviado)
                .verificacionIdentidadRevisadaEn(revisado)
                .motivoRechazoVerificacionIdentidad(null)
                .build();

        assertEquals(EstadoVerificacionIdentidad.EN_PROCESO, p.getEstadoVerificacionIdentidad());
        assertFalse(p.isEsVerificado());
        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg", p.getArchivoCedulaFrontal());
        assertEquals("b2c3d4e5-f6a7-8901-bcde-f12345678901.png", p.getArchivoCedulaReverso());
        assertEquals(enviado, p.getVerificacionIdentidadEnviadaEn());
        assertEquals(revisado, p.getVerificacionIdentidadRevisadaEn());
        assertNull(p.getMotivoRechazoVerificacionIdentidad());
    }
}
