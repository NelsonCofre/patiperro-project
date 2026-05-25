package com.patiperro.paseador.controller;

import com.patiperro.paseador.auth.service.JwtService;
import com.patiperro.paseador.user.dto.PaseadorResumenResponseDTO;
import com.patiperro.paseador.user.service.PaseadorConfiguracionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaseadorConfiguracionPublicController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaseadorConfiguracionPublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaseadorConfiguracionService configuracionService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void obtenerResumenPublico_incluyeEsVerificado() throws Exception {
        when(configuracionService.getResumenPublicoByPaseadorId(eq(7L)))
                .thenReturn(PaseadorResumenResponseDTO.builder()
                        .idPaseador(7L)
                        .nombreCompleto("Ana Pérez")
                        .fotoPerfil("/fotos/ana.jpg")
                        .correo("ana@example.com")
                        .esVerificado(true)
                        .build());

        mockMvc.perform(get("/api/paseadores/public/7/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPaseador").value(7))
                .andExpect(jsonPath("$.esVerificado").value(true));

        verify(configuracionService).getResumenPublicoByPaseadorId(7L);
    }
}
