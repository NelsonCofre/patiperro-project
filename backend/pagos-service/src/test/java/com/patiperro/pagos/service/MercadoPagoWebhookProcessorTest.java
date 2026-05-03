package com.patiperro.pagos.service;

import com.patiperro.pagos.dto.MercadoPagoPaymentDto;
import com.patiperro.pagos.model.EstadoPago;
import com.patiperro.pagos.model.Transaccion;
import com.patiperro.pagos.repository.TransaccionRepository;
import com.patiperro.pagos.support.MercadoPagoApiClient;
import com.patiperro.pagos.support.ReservaPagosIntegracionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoPagoWebhookProcessorTest {

    @Mock
    private MercadoPagoApiClient mercadoPagoApiClient;

    @Mock
    private ReservaPagosIntegracionClient reservaPagosIntegracionClient;

    @Mock
    private TransaccionRepository transaccionRepository;

    @Mock
    private PagoExternoService pagoExternoService;

    private MercadoPagoWebhookProcessor processor;

    @BeforeEach
    void setUp() {
        lenient()
                .when(transaccionRepository.findFirstByIdReservaAndEstadoPagoOrderByIdTransaccionDesc(
                        anyLong(), eq(EstadoPago.PENDIENTE)))
                .thenReturn(Optional.empty());
        processor = new MercadoPagoWebhookProcessor(
                mercadoPagoApiClient,
                reservaPagosIntegracionClient,
                transaccionRepository,
                pagoExternoService);
    }

    private static Transaccion transaccionPendiente(long idTransaccion, long idReserva) {
        Transaccion tx = new Transaccion();
        tx.setIdTransaccion(idTransaccion);
        tx.setIdReserva(idReserva);
        tx.setEstadoPago(EstadoPago.PENDIENTE);
        return tx;
    }

    @Test
    void procesar_aprobado_notificaReserva() {
        when(transaccionRepository.findFirstByIdReservaAndEstadoPagoOrderByIdTransaccionDesc(
                eq(42L), eq(EstadoPago.PENDIENTE)))
                .thenReturn(Optional.of(transaccionPendiente(555L, 42L)));
        MercadoPagoPaymentDto pago = new MercadoPagoPaymentDto(
                999L, "approved", null, "42", java.util.List.of());
        when(mercadoPagoApiClient.obtenerPago("999")).thenReturn(Optional.of(pago));

        processor.procesar("payment", "999");

        verify(reservaPagosIntegracionClient).notificarPagoAprobado(eq(42), eq(555L), eq("999"));
        verify(reservaPagosIntegracionClient, never()).notificarPagoNoAprobado(
                anyInt(), anyString(), anyString(), anyString());
    }

    @Test
    void procesar_rechazado_notificaNoAprobado() {
        MercadoPagoPaymentDto pago = new MercadoPagoPaymentDto(
                1L, "rejected", "cc_rejected", "7", java.util.List.of());
        when(mercadoPagoApiClient.obtenerPago("1")).thenReturn(Optional.of(pago));

        processor.procesar("payment", "1");

        verify(reservaPagosIntegracionClient).notificarPagoNoAprobado(eq(7), eq("1"), eq("rejected"), eq("cc_rejected"));
        verify(reservaPagosIntegracionClient, never()).notificarPagoAprobado(anyInt(), anyLong(), anyString());
    }

    @Test
    void procesar_pending_noNotificaReserva() {
        MercadoPagoPaymentDto pago = new MercadoPagoPaymentDto(
                2L, "pending", null, "7", java.util.List.of());
        when(mercadoPagoApiClient.obtenerPago("2")).thenReturn(Optional.of(pago));

        processor.procesar("payment", "2");

        verify(reservaPagosIntegracionClient, never()).notificarPagoAprobado(anyInt(), anyLong(), anyString());
        verify(reservaPagosIntegracionClient, never()).notificarPagoNoAprobado(
                anyInt(), anyString(), anyString(), anyString());
    }

    @Test
    void procesar_topicDistintoPayment_noConsultaApi() {
        processor.procesar("merchant_order", "123");

        verify(mercadoPagoApiClient, never()).obtenerPago(anyString());
        verify(reservaPagosIntegracionClient, never()).notificarPagoAprobado(anyInt(), anyLong(), anyString());
    }

    @Test
    void procesar_externalReferencePrefijoExtraeIdTrasDosPuntos() {
        when(transaccionRepository.findFirstByIdReservaAndEstadoPagoOrderByIdTransaccionDesc(
                eq(99L), eq(EstadoPago.PENDIENTE)))
                .thenReturn(Optional.of(transaccionPendiente(777L, 99L)));
        MercadoPagoPaymentDto pago = new MercadoPagoPaymentDto(
                5L, "approved", null, "patiperro:99", java.util.List.of());
        when(mercadoPagoApiClient.obtenerPago("5")).thenReturn(Optional.of(pago));

        processor.procesar("payment", "5");

        ArgumentCaptor<Integer> idCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(reservaPagosIntegracionClient).notificarPagoAprobado(idCaptor.capture(), eq(777L), eq("5"));
        assertThat(idCaptor.getValue()).isEqualTo(99);
    }

    @Test
    void procesar_externalReferencePatiperroReserva_prefijoCheckout() {
        when(transaccionRepository.findFirstByIdReservaAndEstadoPagoOrderByIdTransaccionDesc(
                eq(42L), eq(EstadoPago.PENDIENTE)))
                .thenReturn(Optional.of(transaccionPendiente(888L, 42L)));
        MercadoPagoPaymentDto pago = new MercadoPagoPaymentDto(
                6L, "approved", null, "patiperro-reserva:42", java.util.List.of());
        when(mercadoPagoApiClient.obtenerPago("6")).thenReturn(Optional.of(pago));

        processor.procesar("payment", "6");

        verify(reservaPagosIntegracionClient).notificarPagoAprobado(eq(42), eq(888L), eq("6"));
    }
}
