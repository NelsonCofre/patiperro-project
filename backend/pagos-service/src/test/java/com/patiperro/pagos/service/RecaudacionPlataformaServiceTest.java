package com.patiperro.pagos.service;

import com.patiperro.pagos.dto.recaudacion.RecaudacionPeriodoResponse;
import com.patiperro.pagos.model.Transaccion;
import com.patiperro.pagos.repository.RecaudacionPlataformaLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecaudacionPlataformaServiceTest {

    @Mock
    private RecaudacionPlataformaLogRepository repository;

    private RecaudacionPlataformaService service;

    @BeforeEach
    void setUp() {
        service = new RecaudacionPlataformaService(repository);
    }

    @Test
    void registrarCobroAprobado_insertaComisionPositiva() {
        when(repository.insertarLog(
                        any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        service.registrarCobroAprobado(transaccionBase());

        ArgumentCaptor<BigDecimal> comisionCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(repository).insertarLog(
                eq(10L),
                eq(42),
                eq("COBRO_APROBADO"),
                eq(new BigDecimal("10000.00")),
                comisionCaptor.capture(),
                eq(new BigDecimal("9500.00")),
                any(LocalDateTime.class),
                any(LocalDateTime.class));
        assertThat(comisionCaptor.getValue()).isEqualByComparingTo("500.00");
    }

    @Test
    void registrarReembolsoTotal_insertaComisionNegativa() {
        when(repository.insertarLog(
                        any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        service.registrarReembolsoTotal(transaccionBase());

        ArgumentCaptor<BigDecimal> comisionCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(repository).insertarLog(
                eq(10L),
                eq(42),
                eq("REEMBOLSO_TOTAL"),
                eq(new BigDecimal("10000.00")),
                comisionCaptor.capture(),
                eq(new BigDecimal("9500.00")),
                any(LocalDateTime.class),
                any(LocalDateTime.class));
        assertThat(comisionCaptor.getValue()).isEqualByComparingTo("-500.00");
    }

    @Test
    void acumulado_normalizaPeriodoMonth() {
        LocalDateTime desde = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 6, 1, 0, 0);
        when(repository.acumularPorPeriodo("month", desde, hasta))
                .thenReturn(List.of(projection(desde, new BigDecimal("1500.00"), 3L)));

        List<RecaudacionPeriodoResponse> result = service.acumulado("month", desde, hasta);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).periodo()).isEqualTo(desde);
        assertThat(result.get(0).totalComision()).isEqualByComparingTo("1500.00");
        assertThat(result.get(0).totalEventos()).isEqualTo(3L);
    }

    @Test
    void acumulado_rechazaRangoInvalido() {
        LocalDateTime fecha = LocalDateTime.of(2026, 5, 1, 0, 0);

        assertThatThrownBy(() -> service.acumulado("day", fecha, fecha))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Transaccion transaccionBase() {
        Transaccion tx = new Transaccion();
        tx.setIdTransaccion(10L);
        tx.setIdReserva(42L);
        tx.setMontoBruto(new BigDecimal("10000.00"));
        tx.setComisionApp(new BigDecimal("500.00"));
        tx.setMontoNeto(new BigDecimal("9500.00"));
        return tx;
    }

    private static RecaudacionPlataformaLogRepository.RecaudacionPeriodoProjection projection(
            LocalDateTime periodo, BigDecimal totalComision, Long totalEventos) {
        return new RecaudacionPlataformaLogRepository.RecaudacionPeriodoProjection() {
            @Override
            public LocalDateTime getPeriodo() {
                return periodo;
            }

            @Override
            public BigDecimal getTotalComision() {
                return totalComision;
            }

            @Override
            public Long getTotalEventos() {
                return totalEventos;
            }
        };
    }
}
