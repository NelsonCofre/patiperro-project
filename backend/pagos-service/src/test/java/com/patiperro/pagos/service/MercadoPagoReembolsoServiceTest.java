package com.patiperro.pagos.service;

import com.patiperro.pagos.model.PagoExterno;
import com.patiperro.pagos.model.Transaccion;
import com.patiperro.pagos.repository.PagoExternoRepository;
import com.patiperro.pagos.repository.TransaccionRepository;
import com.patiperro.pagos.support.MercadoPagoApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoPagoReembolsoServiceTest {

    @Mock
    private MercadoPagoApiClient mercadoPagoApiClient;

    @Mock
    private PagoExternoRepository pagoExternoRepository;

    @Mock
    private TransaccionRepository transaccionRepository;

    @Mock
    private RecaudacionPlataformaService recaudacionPlataformaService;

    private MercadoPagoReembolsoService service;

    @BeforeEach
    void setUp() {
        service = new MercadoPagoReembolsoService(
                mercadoPagoApiClient,
                pagoExternoRepository,
                transaccionRepository,
                recaudacionPlataformaService);
    }

    @Test
    void procesarReembolsoTotal_siFallaLogRecaudacion_mantieneRespuestaIdempotente() {
        Transaccion tx = new Transaccion();
        tx.setIdTransaccion(100L);
        PagoExterno pagoExterno = PagoExterno.builder()
                .idPagoExterno(10L)
                .transaccion(tx)
                .provider(PagoExternoService.PROVIDER_MERCADOPAGO)
                .providerPaymentId("999")
                .refundProviderId("refund-1")
                .build();
        when(pagoExternoRepository.findByProviderAndProviderPaymentId(
                PagoExternoService.PROVIDER_MERCADOPAGO, "999"))
                .thenReturn(Optional.of(pagoExterno));
        doThrow(new IllegalStateException("tabla recaudacion no disponible"))
                .when(recaudacionPlataformaService)
                .registrarReembolsoTotal(tx);

        int status = service.procesarReembolsoTotal(42, "999", null);

        assertThat(status).isEqualTo(204);
        verify(recaudacionPlataformaService).registrarReembolsoTotal(tx);
    }
}
