package com.patiperro.paseador.user.service;

import com.patiperro.paseador.client.AgendaDisponibilidadClient;
import com.patiperro.paseador.model.EstadoVerificacionIdentidad;
import com.patiperro.paseador.model.Paseador;
import com.patiperro.paseador.repository.PaseadorRepository;
import com.patiperro.paseador.repository.TarifaPaseadorRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaseadorBusquedaServiceVerificadoFilterTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    @Mock
    private PaseadorRepository paseadorRepository;

    @Mock
    private AgendaDisponibilidadClient agendaDisponibilidadClient;

    @Mock
    private TarifaPaseadorRepository tarifaRepository;

    private PaseadorBusquedaService busquedaService;

    @BeforeEach
    void setUp() {
        busquedaService = new PaseadorBusquedaService(paseadorRepository, agendaDisponibilidadClient, tarifaRepository);
        ReflectionTestUtils.setField(busquedaService, "entityManager", entityManager);
        ReflectionTestUtils.setField(busquedaService, "filtrarDisponibleDesdeHoyCercanos", false);
    }

    @Test
    void buscarCercanos_soloVerificados_excluyeNoVerificados() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(
                filaCandidato(1L, 1.0),
                filaCandidato(2L, 2.0)));

        when(paseadorRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(
                paseador(1L, true),
                paseador(2L, false)));
        when(tarifaRepository.findTarifaMinimaByPaseadorId(1L)).thenReturn(5000);

        var resultados = busquedaService.buscarCercanos(
                -33.45, -70.65, 10, 20,
                null, null, null, null,
                true);

        assertEquals(1, resultados.size());
        assertEquals(1L, resultados.getFirst().getIdPaseador());
        assertTrue(resultados.getFirst().isEsVerificado());
        verify(query).setParameter(eq("soloVerificados"), eq(true));
    }

    @Test
    void buscarCercanos_soloVerificadosFalse_noFiltraPorVerificacion() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(
                filaCandidato(1L, 1.0),
                filaCandidato(2L, 2.0)));

        when(paseadorRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(
                paseador(1L, true),
                paseador(2L, false)));
        when(tarifaRepository.findTarifaMinimaByPaseadorId(1L)).thenReturn(5000);
        when(tarifaRepository.findTarifaMinimaByPaseadorId(2L)).thenReturn(6000);

        var resultados = busquedaService.buscarCercanos(
                -33.45, -70.65, 10, 20,
                null, null, null, null,
                false);

        assertEquals(2, resultados.size());
        verify(query).setParameter(eq("soloVerificados"), eq(false));
    }

    private static Object[] filaCandidato(long id, double distanciaKm) {
        return new Object[] { id, distanciaKm, BigDecimal.TEN, -33.45, -70.65 };
    }

    private static Paseador paseador(long id, boolean verificado) {
        return Paseador.builder()
                .id(id)
                .correo("p" + id + "@test.cl")
                .contrasena("hash")
                .primerNombre("Paseador")
                .estadoVerificacionIdentidad(
                        verificado ? EstadoVerificacionIdentidad.APROBADO : EstadoVerificacionIdentidad.SIN_ENVIAR)
                .esVerificado(verificado)
                .build();
    }
}
