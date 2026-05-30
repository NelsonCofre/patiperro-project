package com.patiperro.pagos.service;

import com.patiperro.pagos.dto.recaudacion.RecaudacionPeriodoResponse;
import com.patiperro.pagos.model.TipoEventoRecaudacion;
import com.patiperro.pagos.model.Transaccion;
import com.patiperro.pagos.repository.RecaudacionPlataformaLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecaudacionPlataformaService {

    private static final Logger log = LoggerFactory.getLogger(RecaudacionPlataformaService.class);

    private static final int SCALE_MONEDA = 2;

    private final RecaudacionPlataformaLogRepository repository;

    @Transactional
    public void registrarCobroAprobado(Transaccion tx) {
        registrar(tx, TipoEventoRecaudacion.COBRO_APROBADO, nz(tx != null ? tx.getComisionApp() : null));
    }

    @Transactional
    public void registrarReembolsoTotal(Transaccion tx) {
        // El acumulado admin es neto: un reembolso total revierte la comision previamente recaudada.
        BigDecimal comisionARevertir = nz(tx != null ? tx.getComisionApp() : null).negate();
        registrar(tx, TipoEventoRecaudacion.REEMBOLSO_TOTAL, comisionARevertir);
    }

    @Transactional(readOnly = true)
    public List<RecaudacionPeriodoResponse> acumulado(String periodo, LocalDateTime desde, LocalDateTime hasta) {
        String periodoNormalizado = normalizarPeriodo(periodo);
        // Rango por defecto acotado para evitar consultas historicas amplias accidentales.
        LocalDateTime desdeSeguro = desde != null ? desde : LocalDateTime.now().minusMonths(1);
        LocalDateTime hastaSeguro = hasta != null ? hasta : LocalDateTime.now();
        if (!hastaSeguro.isAfter(desdeSeguro)) {
            throw new IllegalArgumentException("El parametro hasta debe ser posterior a desde");
        }

        return repository.acumularPorPeriodo(periodoNormalizado, desdeSeguro, hastaSeguro).stream()
                .map(p -> new RecaudacionPeriodoResponse(
                        p.getPeriodo(),
                        nz(p.getTotalComision()),
                        p.getTotalEventos()))
                .toList();
    }

    private void registrar(Transaccion tx, TipoEventoRecaudacion tipoEvento, BigDecimal comisionApp) {
        if (tx == null) {
            log.warn("Log de recaudacion omitido: transaccion null (tipoEvento={})", tipoEvento);
            return;
        }
        if (tx.getIdTransaccion() == null || tx.getIdReserva() == null) {
            log.warn(
                    "Log de recaudacion omitido: transaccion sin ids requeridos (idTransaccion={}, idReserva={}, tipoEvento={})",
                    tx.getIdTransaccion(),
                    tx.getIdReserva(),
                    tipoEvento);
            return;
        }
        Integer idReserva = idReservaComoInteger(tx);
        if (idReserva == null) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now();
        int filas = repository.insertarLog(
                tx.getIdTransaccion(),
                idReserva,
                tipoEvento.name(),
                nz(tx.getMontoBruto()),
                nz(comisionApp),
                nz(tx.getMontoNeto()),
                ahora,
                ahora);
        if (filas == 0) {
            log.debug(
                    "Log de recaudacion idempotente ya existente (idTransaccion={}, tipoEvento={})",
                    tx.getIdTransaccion(),
                    tipoEvento);
        }
    }

    private static String normalizarPeriodo(String periodo) {
        if (periodo != null && "month".equalsIgnoreCase(periodo.trim())) {
            return "month";
        }
        return "day";
    }

    private Integer idReservaComoInteger(Transaccion tx) {
        if (tx.getIdReserva() > Integer.MAX_VALUE || tx.getIdReserva() < Integer.MIN_VALUE) {
            log.warn(
                    "Log de recaudacion omitido: idReserva fuera de rango INTEGER (idTransaccion={}, idReserva={})",
                    tx.getIdTransaccion(),
                    tx.getIdReserva());
            return null;
        }
        return tx.getIdReserva().intValue();
    }

    private static BigDecimal nz(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO.setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
        }
        return v.setScale(SCALE_MONEDA, RoundingMode.HALF_UP);
    }
}
