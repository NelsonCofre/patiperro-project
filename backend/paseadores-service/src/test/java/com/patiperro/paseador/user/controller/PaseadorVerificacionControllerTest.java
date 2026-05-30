package com.patiperro.paseador.user.controller;

import com.patiperro.paseador.auth.service.JwtService;
import com.patiperro.paseador.model.EstadoVerificacionIdentidad;
import com.patiperro.paseador.user.dto.VerificacionIdentidadResponseDTO;
import com.patiperro.paseador.user.exception.PaseadorUserExceptionHandler;
import com.patiperro.paseador.user.service.PaseadorVerificacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    void obtenerEstado_retornaJsonDelServicio() throws Exception {
        when(verificacionService.obtenerEstadoAutenticado()).thenReturn(
                VerificacionIdentidadResponseDTO.builder()
                        .estado(EstadoVerificacionIdentidad.SIN_ENVIAR)
                        .estadoEtiqueta("Sin enviar")
                        .puedeSubir(true)
                        .build());

        mockMvc.perform(get("/api/paseadores/me/verificacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SIN_ENVIAR"))
                .andExpect(jsonPath("$.puedeSubir").value(true));
    }

    @Test
    void descargarDocumento_incluyeCabecerasSinCache() throws Exception {
        Path temp = Files.createTempFile("cedula-", ".pdf");
        try {
            Files.write(temp, new byte[]{'%', 'P', 'D', 'F', '-'});
            when(verificacionService.resolverDocumentoAutenticado()).thenReturn(temp);

            mockMvc.perform(get("/api/paseadores/me/verificacion/documento"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate"))
                    .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    @Test
    void descargarDocumentoLegacy_incluyeCabecerasSinCache() throws Exception {
        Path temp = Files.createTempFile("cedula-frontal-", ".jpg");
        try {
            Files.write(temp, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00});
            when(verificacionService.resolverDocumentoAutenticado(eq("frontal"))).thenReturn(temp);

            mockMvc.perform(get("/api/paseadores/me/verificacion/documentos/frontal"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate"))
                    .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    @Test
    void subirDocumento_multipartValido_retorna201() throws Exception {
        when(verificacionService.subirDocumento(any())).thenReturn(
                VerificacionIdentidadResponseDTO.builder()
                        .estado(EstadoVerificacionIdentidad.APROBADO)
                        .estadoEtiqueta("Verificado")
                        .puedeSubir(false)
                        .tieneDocumento(true)
                        .build());

        MockMultipartFile documento = new MockMultipartFile(
                "documento",
                "cedula.pdf",
                "application/pdf",
                new byte[]{'%', 'P', 'D', 'F', '-'});

        mockMvc.perform(multipart("/api/paseadores/me/verificacion/documento")
                        .file(documento))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("APROBADO"))
                .andExpect(jsonPath("$.puedeSubir").value(false));
    }

    @Test
    void subirDocumento_archivoVacio_retorna400() throws Exception {
        MockMultipartFile documentoVacio = new MockMultipartFile(
                "documento",
                "cedula.pdf",
                "application/pdf",
                new byte[0]);

        mockMvc.perform(multipart("/api/paseadores/me/verificacion/documento")
                        .file(documentoVacio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Archivo requerido: documento"));
    }

    @Test
    void subirDocumento_faltaDocumento_retorna400() throws Exception {
        mockMvc.perform(multipart("/api/paseadores/me/verificacion/documento"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Archivo requerido: documento"))
                .andExpect(jsonPath("$.status").value(400));
    }
}
