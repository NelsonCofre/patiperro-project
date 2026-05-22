package com.patiperro.paseador.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoVerificacionIdentidadTest {

    @Test
    void puedeSubirDocumentos_soloSinEnviarYRechazado() {
        assertTrue(EstadoVerificacionIdentidad.SIN_ENVIAR.puedeSubirDocumentos());
        assertTrue(EstadoVerificacionIdentidad.RECHAZADO.puedeSubirDocumentos());
        assertFalse(EstadoVerificacionIdentidad.EN_PROCESO.puedeSubirDocumentos());
        assertFalse(EstadoVerificacionIdentidad.APROBADO.puedeSubirDocumentos());
    }

    @Test
    void esAprobado_soloConstanteAprobado() {
        assertTrue(EstadoVerificacionIdentidad.APROBADO.esAprobado());
        assertFalse(EstadoVerificacionIdentidad.SIN_ENVIAR.esAprobado());
        assertFalse(EstadoVerificacionIdentidad.EN_PROCESO.esAprobado());
        assertFalse(EstadoVerificacionIdentidad.RECHAZADO.esAprobado());
    }

    @Test
    void esDecisionRevisionAdmin_soloAprobadoYRechazado() {
        assertTrue(EstadoVerificacionIdentidad.APROBADO.esDecisionRevisionAdmin());
        assertTrue(EstadoVerificacionIdentidad.RECHAZADO.esDecisionRevisionAdmin());
        assertFalse(EstadoVerificacionIdentidad.SIN_ENVIAR.esDecisionRevisionAdmin());
        assertFalse(EstadoVerificacionIdentidad.EN_PROCESO.esDecisionRevisionAdmin());
    }

    @Test
    void getEtiqueta_coincideConAc3() {
        assertEquals("Verificación en proceso", EstadoVerificacionIdentidad.EN_PROCESO.getEtiqueta());
    }

    @Test
    void name_coincideConCheckFlyway() {
        for (EstadoVerificacionIdentidad estado : EstadoVerificacionIdentidad.values()) {
            assertEquals(estado.name(), estado.name().toUpperCase());
            assertTrue(estado.name().length() <= 20);
        }
    }
}
