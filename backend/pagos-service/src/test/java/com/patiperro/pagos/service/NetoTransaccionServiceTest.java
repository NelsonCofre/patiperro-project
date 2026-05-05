package com.patiperro.pagos.service;

import com.patiperro.pagos.config.ComisionPlataformaProperties;
import com.patiperro.pagos.dto.MercadoPagoPaymentDto;
import com.patiperro.pagos.model.Transaccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class NetoTransaccionServiceTest {

    private NetoTransaccionService service;

    @BeforeEach
    void setUp() {
        ComisionPlataformaProperties props = new ComisionPlataformaProperties();
        props.setPlataformaTasa(new BigDecimal("0.05"));
        service = new NetoTransaccionService(props);
    }

    @Test
    void calcular_cincoPorCiento_clp() {
        NetoTransaccionService.ResultadoNeto r = service.calcular(new BigDecimal("10000.00"));
        assertThat(r.comisionApp()).isEqualByComparingTo("500.00");
        assertThat(r.montoNeto()).isEqualByComparingTo("9500.00");
    }

    @Test
    void resolverMontoBruto_prefiereTransactionAmountMp() {
        MercadoPagoPaymentDto pago = new MercadoPagoPaymentDto(
                1L, "approved", null, "42", new BigDecimal("15000"), null, null, null, java.util.List.of());
        Transaccion tx = new Transaccion();
        tx.setMontoBruto(new BigDecimal("9999.00"));
        assertThat(service.resolverMontoBruto(pago, tx)).isEqualByComparingTo("15000.00");
    }

    @Test
    void resolverMontoBruto_sinMp_usaTransaccion() {
        MercadoPagoPaymentDto pago = new MercadoPagoPaymentDto(
                1L, "approved", null, "42", null, null, null, null, java.util.List.of());
        Transaccion tx = new Transaccion();
        tx.setMontoBruto(new BigDecimal("8800.50"));
        assertThat(service.resolverMontoBruto(pago, tx)).isEqualByComparingTo("8800.50");
    }

    @Test
    void calcular_montoNull_oCero_sinComision() {
        NetoTransaccionService.ResultadoNeto a = service.calcular(null);
        assertThat(a.comisionApp().signum()).isZero();
        assertThat(a.montoNeto().signum()).isZero();

        NetoTransaccionService.ResultadoNeto b = service.calcular(BigDecimal.ZERO);
        assertThat(b.comisionApp().signum()).isZero();
        assertThat(b.montoNeto().signum()).isZero();
    }
}
