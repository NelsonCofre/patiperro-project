package com.patiperro.paseador.model;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    /** Mismos literales que {@code chk_paseador_estado_verificacion_identidad} en V1/V2 Flyway. */
    @Test
    void valoresEnum_coincidenConCheckFlyway() {
        Set<String> enEnum = Stream.of(EstadoVerificacionIdentidad.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        assertEquals(Set.of("SIN_ENVIAR", "EN_PROCESO", "APROBADO", "RECHAZADO"), enEnum);
    }

    @Test
    void mensajeBloqueoSubida_soloEnProceso() {
        assertTrue(EstadoVerificacionIdentidad.EN_PROCESO.mensajeBloqueoSubida().isPresent());
        assertTrue(EstadoVerificacionIdentidad.APROBADO.mensajeBloqueoSubida().isEmpty());
        assertTrue(EstadoVerificacionIdentidad.SIN_ENVIAR.mensajeBloqueoSubida().isEmpty());
        assertTrue(EstadoVerificacionIdentidad.RECHAZADO.mensajeBloqueoSubida().isEmpty());
    }

    @Test
    void puedeCambiarDocumento_soloAprobado() {
        assertTrue(EstadoVerificacionIdentidad.APROBADO.puedeCambiarDocumento());
        assertFalse(EstadoVerificacionIdentidad.SIN_ENVIAR.puedeCambiarDocumento());
        assertFalse(EstadoVerificacionIdentidad.EN_PROCESO.puedeCambiarDocumento());
        assertFalse(EstadoVerificacionIdentidad.RECHAZADO.puedeCambiarDocumento());
    }

    @Test
    void puedeEnviarDocumento_bloqueaSoloEnProceso() {
        assertFalse(EstadoVerificacionIdentidad.EN_PROCESO.puedeEnviarDocumento());
        assertTrue(EstadoVerificacionIdentidad.SIN_ENVIAR.puedeEnviarDocumento());
        assertTrue(EstadoVerificacionIdentidad.RECHAZADO.puedeEnviarDocumento());
        assertTrue(EstadoVerificacionIdentidad.APROBADO.puedeEnviarDocumento());
    }
}
