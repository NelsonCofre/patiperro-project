package com.patiperro.pagos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patiperro.pagos.dto.comprobante.ComprobantePagoResponse;
import com.patiperro.pagos.model.ComprobantePago;
import com.patiperro.pagos.model.EstadoPago;
import com.patiperro.pagos.model.Origen;
import com.patiperro.pagos.model.Destino;
import com.patiperro.pagos.model.TipoTransaccion;
import com.patiperro.pagos.model.Transaccion;
import com.patiperro.pagos.repository.ComprobantePagoRepository;
import com.patiperro.pagos.repository.PagoExternoRepository;
import com.patiperro.pagos.repository.TransaccionRepository;
import com.patiperro.pagos.reserva.ReservaConsultaClient;
import com.patiperro.pagos.support.NotificacionResumenPagoIntegracionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComprobantePagoServiceTest {

    private static final String DISCLAIMER =
            "Resumen de Transacción (informativo). No constituye boleta o factura legal.";
    private static final String ESTADO_FONDOS_TEXTO =
            "Pago confirmado. Fondos retenidos en garantía por Patiperro hasta la finalización del servicio";

    @Mock
    private ReservaConsultaClient reservaConsultaClient;

    @Mock
    private TransaccionRepository transaccionRepository;

    @Mock
    private PagoExternoRepository pagoExternoRepository;

    @Mock
    private ComprobantePagoRepository comprobantePagoRepository;

    @Mock
    private NotificacionResumenPagoIntegracionClient notificacionResumenPagoIntegracionClient;

    private ComprobantePagoService service;

    @BeforeEach
    void setUp() {
        service = new ComprobantePagoService(
                reservaConsultaClient,
                transaccionRepository,
                pagoExternoRepository,
                comprobantePagoRepository,
                new ObjectMapper().findAndRegisterModules(),
                new ComprobantePagoHtmlRenderer(),
                notificacionResumenPagoIntegracionClient
        );
    }

    private static TestingAuthenticationToken authTutor(long id) {
        TestingAuthenticationToken a = new TestingAuthenticationToken(
                id, "n/a", List.of(new SimpleGrantedAuthority("ROLE_TUTOR")));
        a.setAuthenticated(true);
        return a;
    }

    @Test
    void obtenerParaTutor_sinRolTutor_lanza401() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                1L, "n/a", List.of(new SimpleGrantedAuthority("ROLE_PASEADOR")));
        auth.setAuthenticated(true);

        assertThatThrownBy(() -> service.obtenerParaTutor(10L, auth))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void obtenerParaTutor_persistido_transaccionRechazada_lanza409() {
        ComprobantePago cp = comprobantePersistidoEjemplo();
        when(comprobantePagoRepository.findByIdReserva(10L)).thenReturn(Optional.of(cp));

        Transaccion tx = Transaccion.builder()
                .idTransaccion(55L)
                .idReserva(10L)
                .montoBruto(BigDecimal.TEN)
                .comisionApp(BigDecimal.ONE)
                .montoNeto(BigDecimal.TEN)
                .origen(Origen.CLIENTE)
                .destino(Destino.PASEADOR)
                .estadoPago(EstadoPago.RECHAZADO)
                .tipoTransaccion(TipoTransaccion.PAGO_CLIENTE)
                .fechaCreacion(LocalDateTime.now())
                .build();
        when(transaccionRepository.findById(55L)).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> service.obtenerParaTutor(10L, authTutor(1L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(reservaConsultaClient, never()).obtenerComprobanteInterno(any());
    }

    @Test
    void obtenerParaTutor_persistido_transaccionDeOtraReserva_lanza409() {
        ComprobantePago cp = comprobantePersistidoEjemplo();
        when(comprobantePagoRepository.findByIdReserva(10L)).thenReturn(Optional.of(cp));

        Transaccion tx = Transaccion.builder()
                .idTransaccion(55L)
                .idReserva(999L)
                .montoBruto(BigDecimal.TEN)
                .comisionApp(BigDecimal.ONE)
                .montoNeto(BigDecimal.TEN)
                .origen(Origen.CLIENTE)
                .destino(Destino.PASEADOR)
                .estadoPago(EstadoPago.APROBADO)
                .tipoTransaccion(TipoTransaccion.PAGO_CLIENTE)
                .fechaCreacion(LocalDateTime.now())
                .build();
        when(transaccionRepository.findById(55L)).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> service.obtenerParaTutor(10L, authTutor(1L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void obtenerParaTutor_persistido_ok_sinConsultarReserva() {
        ComprobantePago cp = comprobantePersistidoEjemplo();
        when(comprobantePagoRepository.findByIdReserva(10L)).thenReturn(Optional.of(cp));

        Transaccion tx = Transaccion.builder()
                .idTransaccion(55L)
                .idReserva(10L)
                .montoBruto(new BigDecimal("10000.00"))
                .comisionApp(new BigDecimal("500.00"))
                .montoNeto(new BigDecimal("9500.00"))
                .origen(Origen.CLIENTE)
                .destino(Destino.PASEADOR)
                .estadoPago(EstadoPago.APROBADO)
                .tipoTransaccion(TipoTransaccion.PAGO_CLIENTE)
                .fechaCreacion(LocalDateTime.of(2026, 5, 7, 9, 0))
                .build();
        when(transaccionRepository.findById(55L)).thenReturn(Optional.of(tx));

        ComprobantePagoResponse r = service.obtenerParaTutor(10L, authTutor(1L));

        assertThat(r.idOrden()).isEqualTo(55L);
        assertThat(r.paseadorNombre()).isEqualTo("Paseador X");
        verify(reservaConsultaClient, never()).obtenerComprobanteInterno(any());
    }

    @Test
    void obtenerHtmlParaTutor_priorizaHtmlPersistido() {
        ComprobantePago cp = comprobantePersistidoEjemplo();
        cp.setHtmlResumen("<html><body>snapshot</body></html>");
        when(comprobantePagoRepository.findByIdReserva(10L)).thenReturn(Optional.of(cp));

        Transaccion tx = Transaccion.builder()
                .idTransaccion(55L)
                .idReserva(10L)
                .montoBruto(BigDecimal.TEN)
                .comisionApp(BigDecimal.ONE)
                .montoNeto(BigDecimal.TEN)
                .origen(Origen.CLIENTE)
                .destino(Destino.PASEADOR)
                .estadoPago(EstadoPago.APROBADO)
                .tipoTransaccion(TipoTransaccion.PAGO_CLIENTE)
                .fechaCreacion(LocalDateTime.now())
                .build();
        when(transaccionRepository.findById(55L)).thenReturn(Optional.of(tx));

        String html = service.obtenerHtmlParaTutor(10L, authTutor(1L));

        assertThat(html).isEqualTo("<html><body>snapshot</body></html>");
    }

    private static ComprobantePago comprobantePersistidoEjemplo() {
        ComprobantePago cp = new ComprobantePago();
        cp.setIdReserva(10L);
        cp.setIdTutorUsuario(1L);
        cp.setIdTransaccionPagos(55L);
        cp.setTipoDocumento("RESUMEN_TRANSACCION");
        cp.setDisclaimerLegal(DISCLAIMER);
        cp.setIdTransaccionExterna("999999");
        cp.setFechaHoraOperacion(LocalDateTime.of(2026, 5, 7, 9, 0));
        cp.setPaseadorNombre("Paseador X");
        cp.setMascotaNombre("Firulais");
        cp.setFechaPaseo(LocalDate.of(2026, 5, 7));
        cp.setHoraInicio(LocalDateTime.of(2026, 5, 7, 10, 0));
        cp.setHoraFinal(LocalDateTime.of(2026, 5, 7, 11, 0));
        cp.setDuracionMinutos(60L);
        cp.setMoneda("CLP");
        cp.setMontoTotal(new BigDecimal("10000.00"));
        cp.setComisionApp(new BigDecimal("500.00"));
        cp.setMontoNeto(new BigDecimal("9500.00"));
        cp.setEstadoFondos("Estado: " + ESTADO_FONDOS_TEXTO);
        return cp;
    }
}
