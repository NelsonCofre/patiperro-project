package com.patiperro.pagos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patiperro.pagos.dto.disputa.DisputaReservaResponse;
import com.patiperro.pagos.security.JwtService;
import com.patiperro.pagos.service.BilleteraDisputaReservaService;
import com.patiperro.pagos.service.BilleteraService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalBilleteraController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
                "patiperro.pagos.interno.secret=test-interno-disputa-secret",
                "jwt.secret=test-jwt-secret-key-for-internal-billetera-disputa-tests-32",
        })
class InternalBilleteraDisputaControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BilleteraService billeteraService;

    @MockitoBean
    private BilleteraDisputaReservaService billeteraDisputaReservaService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void abrirDisputa_rechazaSinCabecera() throws Exception {
        mockMvc.perform(
                        post("/api/pagos/interno/billetera/disputa/abrir")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new InternalBilleteraController.AbrirDisputaBody(9, "x"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(billeteraDisputaReservaService);
    }

    @Test
    void abrirDisputa_ok() throws Exception {
        LocalDateTime abierto = LocalDateTime.of(2026, 5, 1, 12, 0);
        when(billeteraDisputaReservaService.abrirDisputa(anyInt(), any()))
                .thenReturn(new DisputaReservaResponse(9, true, abierto, null));

        mockMvc.perform(
                        post("/api/pagos/interno/billetera/disputa/abrir")
                                .header(InternalMercadoPagoController.HEADER_INTERNO, "test-interno-disputa-secret")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new InternalBilleteraController.AbrirDisputaBody(9, "motivo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReserva").value(9))
                .andExpect(jsonPath("$.disputaActiva").value(true));
    }

    @Test
    void cerrarDisputa_ok() throws Exception {
        mockMvc.perform(
                        post("/api/pagos/interno/billetera/disputa/cerrar")
                                .header(InternalMercadoPagoController.HEADER_INTERNO, "test-interno-disputa-secret")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new InternalBilleteraController.CerrarDisputaBody(9))))
                .andExpect(status().isNoContent());
    }
}
