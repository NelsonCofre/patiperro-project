package com.patiperro.pagos.controller;

import com.patiperro.pagos.model.EstadoPago;
import com.patiperro.pagos.model.Origen;
import com.patiperro.pagos.model.Destino;
import com.patiperro.pagos.model.TipoTransaccion;
import com.patiperro.pagos.model.Transaccion;
import com.patiperro.pagos.repository.ComprobantePagoRegistroRepository;
import com.patiperro.pagos.repository.PagoExternoRepository;
import com.patiperro.pagos.repository.TransaccionRepository;
import com.patiperro.pagos.reserva.ReservaConsultaClient;
import com.patiperro.pagos.reserva.dto.ReservaComprobanteDto;
import com.patiperro.pagos.security.JwtService;
import com.patiperro.pagos.service.ComprobantePagoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ComprobantePagoController.class)
@ActiveProfiles("test")
@Import(ComprobantePagoService.class)
class ComprobantePagoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaConsultaClient reservaConsultaClient;

    @MockitoBean
    private TransaccionRepository transaccionRepository;

    @MockitoBean
    private PagoExternoRepository pagoExternoRepository;

    @MockitoBean
    private ComprobantePagoRegistroRepository comprobantePagoRegistroRepository;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void stubRegistroComprobante() {
        when(comprobantePagoRegistroRepository.findByIdReserva(anyInt())).thenReturn(Optional.empty());
    }

    @Test
    void sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/api/pagos/comprobante/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tutorNoDuenio_retorna403() throws Exception {
        when(reservaConsultaClient.obtenerComprobanteInterno(eq(10L)))
                .thenReturn(new ReservaComprobanteDto(
                        10,
                        999, // dueño real
                        "tutor@correo.cl",
                        1,
                        "Firulais",
                        2,
                        "Paseador",
                        100,
                        LocalDate.of(2026, 5, 7),
                        LocalDateTime.of(2026, 5, 7, 10, 0),
                        LocalDateTime.of(2026, 5, 7, 11, 0),
                        new BigDecimal("10000.00"),
                        123L));

        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                "1", "n/a", List.of(new SimpleGrantedAuthority("ROLE_TUTOR")));
        auth.setAuthenticated(true);

        mockMvc.perform(get("/api/pagos/comprobante/10")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinPagoAprobado_retorna409() throws Exception {
        when(reservaConsultaClient.obtenerComprobanteInterno(eq(10L)))
                .thenReturn(new ReservaComprobanteDto(
                        10,
                        1,
                        "tutor@correo.cl",
                        1,
                        "Firulais",
                        2,
                        "Paseador",
                        100,
                        LocalDate.of(2026, 5, 7),
                        LocalDateTime.of(2026, 5, 7, 10, 0),
                        LocalDateTime.of(2026, 5, 7, 11, 0),
                        new BigDecimal("10000.00"),
                        null));

        when(transaccionRepository.findFirstByIdReservaAndEstadoPagoOrderByIdTransaccionDesc(eq(10L), eq(EstadoPago.APROBADO)))
                .thenReturn(Optional.empty());

        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                1L, "n/a", List.of(new SimpleGrantedAuthority("ROLE_TUTOR")));
        auth.setAuthenticated(true);

        mockMvc.perform(get("/api/pagos/comprobante/10")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isConflict());
    }

    @Test
    void ok_retorna200_yCamposBasicos() throws Exception {
        when(reservaConsultaClient.obtenerComprobanteInterno(eq(10L)))
                .thenReturn(new ReservaComprobanteDto(
                        10,
                        1,
                        "tutor@correo.cl",
                        1,
                        "Firulais",
                        2,
                        "Paseador X",
                        100,
                        LocalDate.of(2026, 5, 7),
                        LocalDateTime.of(2026, 5, 7, 10, 0),
                        LocalDateTime.of(2026, 5, 7, 11, 0),
                        new BigDecimal("10000.00"),
                        null));

        Transaccion tx = Transaccion.builder()
                .idTransaccion(55L)
                .idReserva(10L)
                .idPago(999999L)
                .montoBruto(new BigDecimal("10000.00"))
                .comisionApp(new BigDecimal("500.00"))
                .montoNeto(new BigDecimal("9500.00"))
                .origen(Origen.CLIENTE)
                .destino(Destino.PASEADOR)
                .estadoPago(EstadoPago.APROBADO)
                .tipoTransaccion(TipoTransaccion.PAGO_CLIENTE)
                .fechaCreacion(LocalDateTime.of(2026, 5, 7, 9, 0))
                .build();

        when(transaccionRepository.findFirstByIdReservaAndEstadoPagoOrderByIdTransaccionDesc(eq(10L), eq(EstadoPago.APROBADO)))
                .thenReturn(Optional.of(tx));
        when(pagoExternoRepository.findByTransaccion_IdTransaccion(eq(55L)))
                .thenReturn(Optional.empty());

        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                1L, "n/a", List.of(new SimpleGrantedAuthority("ROLE_TUTOR")));
        auth.setAuthenticated(true);

        mockMvc.perform(get("/api/pagos/comprobante/10")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoDocumento").value("RESUMEN_TRANSACCION"))
                .andExpect(jsonPath("$.idReserva").value(10))
                .andExpect(jsonPath("$.idOrden").value(55))
                .andExpect(jsonPath("$.moneda").value("CLP"))
                .andExpect(jsonPath("$.montoTotal").value(10000.00))
                .andExpect(jsonPath("$.duracionMinutos").value(60));
    }
}

