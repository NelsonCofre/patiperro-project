package com.patiperro.reserva.service;

import com.patiperro.reserva.model.EstadoReserva;
import com.patiperro.reserva.model.EstadoReservaCatalogo;
import com.patiperro.reserva.model.Reserva;
import com.patiperro.reserva.repository.ReservaRepository;
import com.patiperro.reserva.support.NotificacionReembolsoIntegracionClient;
import com.patiperro.reserva.support.PagosReembolsoIntegracionClient;
import com.patiperro.reserva.support.TutorIntegracionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservaReembolsoServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private PagosReembolsoIntegracionClient pagosReembolsoIntegracionClient;

    @Mock
    private NotificacionReembolsoIntegracionClient notificacionReembolsoIntegracionClient;

    @Mock
    private TutorIntegracionClient tutorIntegracionClient;

    @Mock
    private PlatformTransactionManager platformTransactionManager;

    private ReservaReembolsoService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-02T12:00:00Z"), ZoneOffset.UTC);
        TransactionStatus txStatus = mock(TransactionStatus.class);
        when(platformTransactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(txStatus);
        doNothing().when(platformTransactionManager).commit(txStatus);
        doNothing().when(platformTransactionManager).rollback(txStatus);
        service = new ReservaReembolsoService(
                reservaRepository,
                pagosReembolsoIntegracionClient,
                notificacionReembolsoIntegracionClient,
                tutorIntegracionClient,
                clock,
                platformTransactionManager);
    }

    @Test
    void requiereReembolsoPorEstado_aceptada_esFalse() {
        EstadoReserva e = new EstadoReserva();
        e.setIdEstadoReserva(EstadoReservaCatalogo.ID_ACEPTADA);
        assertThat(service.requiereReembolsoPorEstado(e)).isFalse();
    }

    @Test
    void requiereReembolsoPorEstado_rechazada_esTrue() {
        EstadoReserva e = new EstadoReserva();
        e.setIdEstadoReserva(EstadoReservaCatalogo.ID_RECHAZADA);
        assertThat(service.requiereReembolsoPorEstado(e)).isTrue();
    }

    @Test
    void requiereReembolsoPorEstado_expirada_esTrue() {
        EstadoReserva e = new EstadoReserva();
        e.setIdEstadoReserva(EstadoReservaCatalogo.ID_EXPIRADA);
        assertThat(service.requiereReembolsoPorEstado(e)).isTrue();
    }

    @Test
    void requiereReembolsoPorEstado_cancelada_esTrue() {
        EstadoReserva e = new EstadoReserva();
        e.setIdEstadoReserva(EstadoReservaCatalogo.ID_CANCELADA);
        assertThat(service.requiereReembolsoPorEstado(e)).isTrue();
    }

    @Test
    void procesarReembolsoYNotificarSync_cuandoYaMarcado_noLlamaPagos() {
        Reserva r = reservaRechazadaConPago();
        r.setMercadopagoReembolsoProcesadoEn(LocalDateTime.now());
        when(reservaRepository.findById(1)).thenReturn(Optional.of(r));

        service.procesarReembolsoYNotificarSync(1);

        verify(pagosReembolsoIntegracionClient, never()).solicitarReembolsoTotal(any(), any());
    }

    @Test
    void procesarReembolsoYNotificarSync_respuesta204_marcaReembolsoConCatalogoDeEstados() {
        Reserva r = reservaRechazadaConPago();
        when(reservaRepository.findById(1)).thenReturn(Optional.of(r));
        when(pagosReembolsoIntegracionClient.isEnabled()).thenReturn(true);
        when(pagosReembolsoIntegracionClient.solicitarReembolsoTotal(1, "mp-1")).thenReturn(204);
        when(reservaRepository.marcarMercadopagoReembolsoProcesadoSiPendiente(
                eq(1), any(LocalDateTime.class), eq(EstadoReservaCatalogo.IDS_ESTADO_REEMBOLSO_MERCADOPAGO)))
                .thenReturn(1);
        when(notificacionReembolsoIntegracionClient.isEnabled()).thenReturn(false);

        service.procesarReembolsoYNotificarSync(1);

        verify(reservaRepository).marcarMercadopagoReembolsoProcesadoSiPendiente(
                eq(1), any(LocalDateTime.class), eq(EstadoReservaCatalogo.IDS_ESTADO_REEMBOLSO_MERCADOPAGO));
    }

    private static Reserva reservaRechazadaConPago() {
        EstadoReserva est = new EstadoReserva();
        est.setIdEstadoReserva(EstadoReservaCatalogo.ID_RECHAZADA);
        Reserva r = new Reserva();
        r.setIdReserva(1);
        r.setIdTutorUsuario(9);
        r.setEstadoReserva(est);
        r.setMercadopagoPaymentId("mp-1");
        r.setMercadopagoReembolsoProcesadoEn(null);
        return r;
    }
}
