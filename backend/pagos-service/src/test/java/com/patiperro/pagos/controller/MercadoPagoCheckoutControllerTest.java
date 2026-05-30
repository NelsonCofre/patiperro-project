package com.patiperro.pagos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patiperro.pagos.config.MercadoPagoCheckoutProperties;
import com.patiperro.pagos.dto.checkout.CrearPreferenciaRequest;
import com.patiperro.pagos.security.JwtService;
import com.patiperro.pagos.service.MercadoPagoCheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MercadoPagoCheckoutController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableConfigurationProperties(MercadoPagoCheckoutProperties.class)
@TestPropertySource(
        properties = {
                "patiperro.pagos.interno.secret=test-interno-secret",
                "patiperro.mercadopago.checkout.use-sandbox=true",
                "jwt.secret=test-jwt-secret-key-for-webmvc-pagos-checkout-tests-32"
        })
class MercadoPagoCheckoutControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MercadoPagoCheckoutService mercadoPagoCheckoutService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void crearPreferencia_sinSecreto_retorna403() throws Exception {
        mockMvc.perform(post("/api/pagos/interno/mercadopago/checkout/preferencia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearPreferencia_secretoIncorrecto_retorna403() throws Exception {
        mockMvc.perform(post("/api/pagos/interno/mercadopago/checkout/preferencia")
                        .header(InternalMercadoPagoController.HEADER_INTERNO, "mal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearPreferencia_cuerpoInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/pagos/interno/mercadopago/checkout/preferencia")
                        .header(InternalMercadoPagoController.HEADER_INTERNO, "test-interno-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearPreferencia_ok_retornaPreferenciaYUrlSandbox() throws Exception {
        when(mercadoPagoCheckoutService.crearPreferenciaReserva(
                        eq(1), any(BigDecimal.class), nullable(String.class), nullable(String.class)))
                .thenReturn(Optional.of(new MercadoPagoCheckoutService.PreferenciaCheckoutCreada(
                        "pref-1",
                        "https://www.mercadopago.com/checkout/v1/redirect?pref_id=pref-1",
                        "https://sandbox.mercadopago.com/checkout/v1/redirect?pref_id=pref-1")));

        CrearPreferenciaRequest body = new CrearPreferenciaRequest(1, new BigDecimal("100.00"), "Reserva");

        mockMvc.perform(post("/api/pagos/interno/mercadopago/checkout/preferencia")
                        .header(InternalMercadoPagoController.HEADER_INTERNO, "test-interno-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferenceId").value("pref-1"))
                .andExpect(jsonPath("$.urlCheckout").value("https://sandbox.mercadopago.com/checkout/v1/redirect?pref_id=pref-1"));
    }

    @Test
    void crearPreferencia_servicioVacio_retorna503() throws Exception {
        when(mercadoPagoCheckoutService.crearPreferenciaReserva(
                        anyInt(), any(BigDecimal.class), nullable(String.class), nullable(String.class)))
                .thenReturn(Optional.empty());

        CrearPreferenciaRequest body = new CrearPreferenciaRequest(1, new BigDecimal("10.00"), null);

        mockMvc.perform(post("/api/pagos/interno/mercadopago/checkout/preferencia")
                        .header(InternalMercadoPagoController.HEADER_INTERNO, "test-interno-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isServiceUnavailable());
    }
}
