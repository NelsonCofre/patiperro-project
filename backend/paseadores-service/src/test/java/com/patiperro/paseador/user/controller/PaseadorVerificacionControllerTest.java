package com.patiperro.paseador.user.controller;

import com.patiperro.paseador.auth.service.JwtService;
import com.patiperro.paseador.model.EstadoVerificacionIdentidad;
import com.patiperro.paseador.user.dto.VerificacionIdentidadResponseDTO;
import com.patiperro.paseador.user.exception.PaseadorUserExceptionHandler;
import com.patiperro.paseador.user.service.PaseadorVerificacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaseadorVerificacionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PaseadorUserExceptionHandler.class)
class PaseadorVerificacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaseadorVerificacionService verificacionService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void subirDocumentos_multipartValido_retorna201() throws Exception {
        when(verificacionService.subirDocumentos(any(), any())).thenReturn(
                VerificacionIdentidadResponseDTO.builder()
                        .estado(EstadoVerificacionIdentidad.EN_PROCESO)
                        .estadoEtiqueta("Verificación en proceso")
                        .puedeSubir(false)
                        .build());

        MockMultipartFile frontal = new MockMultipartFile(
                "cedulaFrontal",
                "frontal.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00});
        MockMultipartFile reverso = new MockMultipartFile(
                "cedulaReverso",
                "reverso.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00});

        mockMvc.perform(multipart("/api/paseadores/me/verificacion/documentos")
                        .file(frontal)
                        .file(reverso))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"))
                .andExpect(jsonPath("$.puedeSubir").value(false));
    }

    @Test
    void subirDocumentos_archivoVacio_retorna400() throws Exception {
        MockMultipartFile frontalVacio = new MockMultipartFile(
                "cedulaFrontal",
                "frontal.jpg",
                "image/jpeg",
                new byte[0]);
        MockMultipartFile reverso = new MockMultipartFile(
                "cedulaReverso",
                "reverso.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00});

        mockMvc.perform(multipart("/api/paseadores/me/verificacion/documentos")
                        .file(frontalVacio)
                        .file(reverso))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Archivo requerido: cedulaFrontal"));
    }
}
