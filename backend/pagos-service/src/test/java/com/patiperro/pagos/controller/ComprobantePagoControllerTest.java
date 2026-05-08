package com.patiperro.pagos.controller;

import com.patiperro.pagos.dto.comprobante.ComprobantePagoResponse;
import com.patiperro.pagos.security.JwtService;
import com.patiperro.pagos.service.ComprobantePagoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ComprobantePagoController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
                "jwt.secret=test-jwt-secret-key-for-webmvc-pagos-comprobante-tests-32bytes!"
        })
class ComprobantePagoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComprobantePagoService comprobantePagoService;

    @MockitoBean
    private JwtService jwtService;

    private static TestingAuthenticationToken authTutor(long tutorUsuarioId) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                tutorUsuarioId, "n/a", List.of(new SimpleGrantedAuthority("ROLE_TUTOR")));
        auth.setAuthenticated(true);
        return auth;
    }

    @Test
    void tutorNoDuenio_retorna403() throws Exception {
        when(comprobantePagoService.obtenerParaTutor(eq(10L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/api/pagos/comprobante/10")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authTutor(1L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinPagoAprobado_retorna409() throws Exception {
        when(comprobantePagoService.obtenerParaTutor(eq(10L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "No existe pago aprobado"));

        mockMvc.perform(get("/api/pagos/comprobante/10")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authTutor(1L))))
                .andExpect(status().isConflict());
    }

    @Test
    void ok_retorna200_yCamposBasicos() throws Exception {
        ComprobantePagoResponse dto = new ComprobantePagoResponse(
                "RESUMEN_TRANSACCION",
                "disc",
                10L,
                55L,
                "999999",
                LocalDateTime.of(2026, 5, 7, 9, 0),
                "Paseador X",
                "Firulais",
                LocalDate.of(2026, 5, 7),
                LocalDateTime.of(2026, 5, 7, 10, 0),
                LocalDateTime.of(2026, 5, 7, 11, 0),
                60L,
                "CLP",
                new BigDecimal("10000.00"),
                new BigDecimal("500.00"),
                new BigDecimal("9500.00"),
                "Estado: ok");

        when(comprobantePagoService.obtenerParaTutor(eq(10L), any())).thenReturn(dto);

        mockMvc.perform(get("/api/pagos/comprobante/10")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authTutor(1L)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoDocumento").value("RESUMEN_TRANSACCION"))
                .andExpect(jsonPath("$.idReserva").value(10))
                .andExpect(jsonPath("$.idOrden").value(55))
                .andExpect(jsonPath("$.moneda").value("CLP"))
                .andExpect(jsonPath("$.montoTotal").value(10000.00))
                .andExpect(jsonPath("$.duracionMinutos").value(60));
    }

    @Test
    void html_ok_retorna200_attachmentYContenidoHtml() throws Exception {
        String html = "<!DOCTYPE html><html><body><h1>Resumen de comprobante</h1></body></html>";
        when(comprobantePagoService.obtenerHtmlParaTutor(eq(10L), any())).thenReturn(html);

        mockMvc.perform(get("/api/pagos/comprobante/10/html")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authTutor(1L))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("comprobante-reserva-10.html")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Resumen de comprobante")));
    }
}
