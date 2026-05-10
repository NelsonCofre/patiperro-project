package com.patiperro.pagos.service;

import com.patiperro.pagos.config.ComisionPlataformaProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class NetoTransaccionServiceRoundingTest {

    @Test
    void calcular_redondeaComisionHaciaArribaCuandoDecimalEsMedioOCinco() {
        NetoTransaccionService service = serviceWithTasa("0.05");

        NetoTransaccionService.ResultadoNeto resultado =
                service.calcular(new BigDecimal("100.10"));

        assertThat(resultado.comisionApp()).isEqualByComparingTo("5.01");
        assertThat(resultado.montoNeto()).isEqualByComparingTo("95.09");
        assertThat(service.montosCierranIntegridad(
                new BigDecimal("100.10"),
                resultado.comisionApp(),
                resultado.montoNeto()))
                .isTrue();
    }

    @Test
    void calcular_redondeaComisionHaciaArribaConTasaQuincePorCiento() {
        NetoTransaccionService service = serviceWithTasa("0.15");

        NetoTransaccionService.ResultadoNeto resultado =
                service.calcular(new BigDecimal("9999.99"));

        assertThat(resultado.comisionApp()).isEqualByComparingTo("1500.00");
        assertThat(resultado.montoNeto()).isEqualByComparingTo("8499.99");
        assertThat(service.montosCierranIntegridad(
                new BigDecimal("9999.99"),
                resultado.comisionApp(),
                resultado.montoNeto()))
                .isTrue();
    }

    @Test
    void calcular_redondeaComisionHaciaArribaConTasaVeintePorCiento() {
        NetoTransaccionService service = serviceWithTasa("0.20");

        NetoTransaccionService.ResultadoNeto resultado =
                service.calcular(new BigDecimal("3333.33"));

        assertThat(resultado.comisionApp()).isEqualByComparingTo("666.67");
        assertThat(resultado.montoNeto()).isEqualByComparingTo("2666.66");
        assertThat(service.montosCierranIntegridad(
                new BigDecimal("3333.33"),
                resultado.comisionApp(),
                resultado.montoNeto()))
                .isTrue();
    }

    @Test
    void calcular_redondeaMontoBrutoAntesDeCalcularComision() {
        NetoTransaccionService service = serviceWithTasa("0.10");

        NetoTransaccionService.ResultadoNeto resultado =
                service.calcular(new BigDecimal("100.005"));

        assertThat(resultado.comisionApp()).isEqualByComparingTo("10.00");
        assertThat(resultado.montoNeto()).isEqualByComparingTo("90.01");
        assertThat(service.montosCierranIntegridad(
                new BigDecimal("100.01"),
                resultado.comisionApp(),
                resultado.montoNeto()))
                .isTrue();
    }

    private static NetoTransaccionService serviceWithTasa(String tasa) {
        ComisionPlataformaProperties props = new ComisionPlataformaProperties();
        props.setPlataformaTasa(new BigDecimal(tasa));
        return new NetoTransaccionService(props);
    }
}
