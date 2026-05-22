package com.patiperro.paseador.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void verificacionIdentidad_sinJwt_rechazada() throws Exception {
        mockMvc.perform(get("/api/paseadores/me/verificacion"))
                .andExpect(status().isForbidden());
    }

    @Test
    void documentosVerificacion_sinJwt_rechazada() throws Exception {
        mockMvc.perform(get("/api/paseadores/me/verificacion/documentos/frontal"))
                .andExpect(status().isForbidden());
    }

    @Test
    void recursosPublicos_sinJwt_permitidos() throws Exception {
        mockMvc.perform(get("/api/paseadores/public/tamanos"))
                .andExpect(status().isOk());
    }
}
