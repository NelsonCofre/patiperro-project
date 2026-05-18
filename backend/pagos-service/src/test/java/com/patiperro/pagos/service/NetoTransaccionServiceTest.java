package com.patiperro.pagos.service;

import com.patiperro.pagos.config.ComisionPlataformaProperties;
import com.patiperro.pagos.dto.MercadoPagoPaymentDto;
import com.patiperro.pagos.model.Transaccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

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

    @Test
    void montosCierranIntegridad_cierraTrasCalcular() {
        BigDecimal bruto = new BigDecimal("10000.00");
        NetoTransaccionService.ResultadoNeto r = service.calcular(bruto);
        assertThat(service.montosCierranIntegridad(bruto, r.comisionApp(), r.montoNeto())).isTrue();
    }

    @Test
    void montosCierranIntegridad_detectaDescuadre() {
        assertThat(service.montosCierranIntegridad(
                        new BigDecimal("10000.00"),
                        new BigDecimal("500.00"),
                        new BigDecimal("9400.00")))
                .isFalse();
    }

    @Test
    void montosCierranIntegridad_nulosComoCero() {
        assertThat(service.montosCierranIntegridad(null, null, null)).isTrue();
    }

    @Test
    void tasaPlataformaFraccionParaExposicion_alineadaAConfig() {
        assertThat(service.tasaPlataformaFraccionParaExposicion()).isEqualByComparingTo("0.050000");
    }

    @Test
    void calcular_quincePorCiento_clp() {
        NetoTransaccionService s = serviceWithTasa(new BigDecimal("0.15"));
        NetoTransaccionService.ResultadoNeto r = s.calcular(new BigDecimal("10000.00"));
        assertThat(r.comisionApp()).isEqualByComparingTo("1500.00");
        assertThat(r.montoNeto()).isEqualByComparingTo("8500.00");
    }

    @Test
    void calcular_veintePorCiento_clp() {
        NetoTransaccionService s = serviceWithTasa(new BigDecimal("0.20"));
        NetoTransaccionService.ResultadoNeto r = s.calcular(new BigDecimal("10000.00"));
        assertThat(r.comisionApp()).isEqualByComparingTo("2000.00");
        assertThat(r.montoNeto()).isEqualByComparingTo("8000.00");
    }

    @Test
    void montosCierranIntegridad_cierraParaVariasTasas_usandoCalcular() {
        BigDecimal bruto = new BigDecimal("3333.33");
        for (String tasaStr : List.of("0.05", "0.15", "0.20")) {
            NetoTransaccionService s = serviceWithTasa(new BigDecimal(tasaStr));
            NetoTransaccionService.ResultadoNeto r = s.calcular(bruto);
            assertThat(s.montosCierranIntegridad(bruto, r.comisionApp(), r.montoNeto())).isTrue();
        }
    }

    @Test
    void tasaPlataformaFraccionParaExposicion_acotaSuperiorAUno() {
        NetoTransaccionService s = serviceWithTasa(new BigDecimal("2"));
        assertThat(s.tasaPlataformaFraccionParaExposicion()).isEqualByComparingTo("1.000000");
        NetoTransaccionService.ResultadoNeto r = s.calcular(new BigDecimal("100.00"));
        assertThat(r.comisionApp()).isEqualByComparingTo("100.00");
        assertThat(r.montoNeto()).isEqualByComparingTo("0.00");
    }

    @Test
    void tasaPlataformaFraccionParaExposicion_acotaInferiorACero() {
        NetoTransaccionService s = serviceWithTasa(new BigDecimal("-0.50"));
        assertThat(s.tasaPlataformaFraccionParaExposicion()).isEqualByComparingTo("0.000000");
        NetoTransaccionService.ResultadoNeto r = s.calcular(new BigDecimal("5000.00"));
        assertThat(r.comisionApp()).isEqualByComparingTo("0.00");
        assertThat(r.montoNeto()).isEqualByComparingTo("5000.00");
    }

    private static NetoTransaccionService serviceWithTasa(BigDecimal tasa) {
        ComisionPlataformaProperties props = new ComisionPlataformaProperties();
        props.setPlataformaTasa(tasa);
        return new NetoTransaccionService(props);
    }
}
