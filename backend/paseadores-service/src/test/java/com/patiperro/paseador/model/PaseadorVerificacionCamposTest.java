package com.patiperro.paseador.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaseadorVerificacionCamposTest {

    @Test
    void builder_defaultEstadoSinEnviarYArchivosNull() {
        Paseador p = Paseador.builder()
                .correo("nuevo@test.cl")
                .contrasena("hash")
                .build();

        assertEquals(EstadoVerificacionIdentidad.SIN_ENVIAR, p.getEstadoVerificacionIdentidad());
        assertNull(p.getArchivoCedulaFrontal());
        assertNull(p.getArchivoCedulaReverso());
        assertNull(p.getVerificacionIdentidadEnviadaEn());
        assertNull(p.getMotivoRechazoVerificacionIdentidad());
    }
}
