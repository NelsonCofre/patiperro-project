package com.patiperro.paseador.controller;

import com.patiperro.paseador.auth.service.JwtService;
import com.patiperro.paseador.user.dto.PaseadorCercanoResponseDTO;
import com.patiperro.paseador.user.dto.PaseadorCercanosConConteoResponseDTO;
import com.patiperro.paseador.user.service.PaseadorBusquedaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaseadorBusquedaController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaseadorBusquedaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaseadorBusquedaService paseadorBusquedaService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void listarCercanos_sinSoloVerificados_delegaFalsePorDefecto() throws Exception {
        when(paseadorBusquedaService.buscarCercanos(
                eq(-33.45),
                eq(-70.65),
                eq(10.0),
                eq(20),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(false)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/paseadores/public/cercanos")
                        .param("latitudReferencia", "-33.45")
                        .param("longitudReferencia", "-70.65"))
                .andExpect(status().isOk());

        verify(paseadorBusquedaService).buscarCercanos(
                eq(-33.45),
                eq(-70.65),
                eq(10.0),
                eq(20),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(false));
    }

    @Test
    void listarCercanos_soloVerificados_delegaAlServicio() throws Exception {
        when(paseadorBusquedaService.buscarCercanos(
                eq(-33.45),
                eq(-70.65),
                eq(10.0),
                eq(20),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(true)))
                .thenReturn(List.of(PaseadorCercanoResponseDTO.builder()
                        .idPaseador(1L)
                        .nombreCompleto("Felipe")
                        .distanciaKm(1.2)
                        .verificado(true)
                        .build()));

        mockMvc.perform(get("/api/paseadores/public/cercanos")
                        .param("latitudReferencia", "-33.45")
                        .param("longitudReferencia", "-70.65")
                        .param("soloVerificados", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].verificado").value(true));

        verify(paseadorBusquedaService).buscarCercanos(
                eq(-33.45),
                eq(-70.65),
                eq(10.0),
                eq(20),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(true));
    }

    @Test
    void listarCercanosConConteo_soloVerificados_delegaAlServicio() throws Exception {
        when(paseadorBusquedaService.buscarCercanosConConteo(
                eq(-33.45),
                eq(-70.65),
                eq(10.0),
                eq(0),
                eq(20),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(true)))
                .thenReturn(PaseadorCercanosConConteoResponseDTO.builder()
                        .totalDisponibles(1)
                        .offset(0)
                        .limit(20)
                        .resultados(List.of())
                        .build());

        mockMvc.perform(get("/api/paseadores/public/cercanos-con-conteo")
                        .param("latitudReferencia", "-33.45")
                        .param("longitudReferencia", "-70.65")
                        .param("soloVerificados", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDisponibles").value(1));

        verify(paseadorBusquedaService).buscarCercanosConConteo(
                eq(-33.45),
                eq(-70.65),
                eq(10.0),
                eq(0),
                eq(20),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(true));
    }
}
