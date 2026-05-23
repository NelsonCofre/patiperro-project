package com.patiperro.paseador.controller;

import com.patiperro.paseador.auth.service.JwtService;
import com.patiperro.paseador.model.EstadoVerificacionIdentidad;
import com.patiperro.paseador.model.Paseador;
import com.patiperro.paseador.repository.PaseadorRepository;
import com.patiperro.paseador.user.dto.VerificacionIdentidadResponseDTO;
import com.patiperro.paseador.user.service.PaseadorVerificacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaseadorInternoController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "patiperro.paseadores.interno.secret=test-interno-secret")
class PaseadorInternoControllerTest {

    private static final String SECRETO_VALIDO = "test-interno-secret";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaseadorRepository paseadorRepository;

    @MockitoBean
    private PaseadorVerificacionService verificacionService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void revisarVerificacion_sinSecreto_retorna403() throws Exception {
        mockMvc.perform(put("/api/paseadores/interno/1/verificacion-identidad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"APROBADO"}
                                """))
                .andExpect(status().isForbidden());

        verify(verificacionService, never()).revisarVerificacionInterna(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void revisarVerificacion_secretoIncorrecto_retorna403() throws Exception {
        mockMvc.perform(put("/api/paseadores/interno/1/verificacion-identidad")
                        .header(PaseadorInternoController.HEADER_INTERNO, "mal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"APROBADO"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void revisarVerificacion_secretoValido_retorna200() throws Exception {
        when(verificacionService.revisarVerificacionInterna(
                eq(2L), eq(EstadoVerificacionIdentidad.APROBADO), eq(null)))
                .thenReturn(VerificacionIdentidadResponseDTO.builder()
                        .estado(EstadoVerificacionIdentidad.APROBADO)
                        .puedeSubir(false)
                        .build());

        mockMvc.perform(put("/api/paseadores/interno/2/verificacion-identidad")
                        .header(PaseadorInternoController.HEADER_INTERNO, SECRETO_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"APROBADO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));
    }

    @Test
    void descargarDocumentoVerificacion_secretoValido_incluyeNoStore() throws Exception {
        Path temp = Files.createTempFile("cedula-interno-", ".jpg");
        try {
            Files.write(temp, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00});
            when(verificacionService.resolverDocumentoPorPaseadorId(eq(3L), eq("frontal")))
                    .thenReturn(temp);

            mockMvc.perform(get("/api/paseadores/interno/3/verificacion-identidad/documentos/frontal")
                            .header(PaseadorInternoController.HEADER_INTERNO, SECRETO_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate"))
                    .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"));
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    @Test
    void obtenerCorreo_secretoValido_retorna200() throws Exception {
        when(paseadorRepository.findById(4L)).thenReturn(Optional.of(
                Paseador.builder().id(4L).correo("  paseador@test.cl  ").contrasena("hash").build()));

        mockMvc.perform(get("/api/paseadores/interno/4/correo")
                        .header(PaseadorInternoController.HEADER_INTERNO, SECRETO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("paseador@test.cl"));
    }
}
