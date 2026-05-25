package com.patiperro.paseador.user.service;

import com.patiperro.paseador.model.EstadoVerificacionIdentidad;
import com.patiperro.paseador.model.Paseador;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaseadorVerificacionBadgePublicoTest {

    @Test
    void esVerificadoPublicamente_soloCuandoAprobado() {
        assertTrue(esVerificado(EstadoVerificacionIdentidad.APROBADO));
        assertFalse(esVerificado(EstadoVerificacionIdentidad.SIN_ENVIAR));
        assertFalse(esVerificado(EstadoVerificacionIdentidad.EN_PROCESO));
        assertFalse(esVerificado(EstadoVerificacionIdentidad.RECHAZADO));
    }

    @Test
    void esVerificadoPublicamente_paseadorNull_oEstadoNull_retornaFalse() {
        assertFalse(PaseadorVerificacionService.esVerificadoPublicamente(null));
        Paseador sinEstado = Paseador.builder().correo("a@b.com").build();
        sinEstado.setEstadoVerificacionIdentidad(null);
        assertFalse(PaseadorVerificacionService.esVerificadoPublicamente(sinEstado));
    }

    @Test
    void esVerificadoPublicamente_enumAprobadoSinBoolean_retornaTrue() {
        Paseador paseador = Paseador.builder()
                .correo("legacy@test.cl")
                .estadoVerificacionIdentidad(EstadoVerificacionIdentidad.APROBADO)
                .esVerificado(false)
                .build();

        assertTrue(PaseadorVerificacionService.esVerificadoPublicamente(paseador));
    }

    private static boolean esVerificado(EstadoVerificacionIdentidad estado) {
        return PaseadorVerificacionService.esVerificadoPublicamente(
                Paseador.builder()
                        .correo("paseador@test.com")
                        .estadoVerificacionIdentidad(estado)
                        .esVerificado(estado != null && estado.esAprobado())
                        .build());
    }
}
