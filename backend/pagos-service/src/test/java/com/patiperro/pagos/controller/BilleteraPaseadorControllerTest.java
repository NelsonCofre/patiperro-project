package com.patiperro.pagos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patiperro.pagos.dto.billetera.RetiroPaseadorResponse;
import com.patiperro.pagos.security.JwtService;
import com.patiperro.pagos.service.BilleteraService;
import com.patiperro.pagos.service.RetiroPaseadorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BilleteraPaseadorController.class)
@ActiveProfiles("test")
class BilleteraPaseadorControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BilleteraService billeteraService;

    @MockitoBean
    private RetiroPaseadorService retiroPaseadorService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void solicitarRetiro_incluyeMensajeEnRespuesta() throws Exception {
        when(retiroPaseadorService.solicitarRetiro(eq(1L), any()))
                .thenReturn(new RetiroPaseadorResponse(
                        10L,
                        new BigDecimal("1200.00"),
                        new BigDecimal("3800.00"),
                        "ok"));

        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                "1",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_PASEADOR")));
        auth.setAuthenticated(true);

        mockMvc.perform(
                        post("/api/pagos/paseador/billetera/retiros")
                                .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new MontoBody("1200.00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTransaccion").value(10))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    private record MontoBody(String monto) {
    }
}

