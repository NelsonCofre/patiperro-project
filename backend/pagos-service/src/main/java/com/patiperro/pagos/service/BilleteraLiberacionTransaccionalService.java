package com.patiperro.pagos.service;

import com.patiperro.pagos.model.Billetera;
import com.patiperro.pagos.model.BilleteraReservaFase;
import com.patiperro.pagos.model.BilleteraReservaTracking;
import com.patiperro.pagos.model.BilleteraSaldoAuditoria;
import com.patiperro.pagos.repository.BilleteraDisputaReservaRepository;
import com.patiperro.pagos.repository.BilleteraRepository;
import com.patiperro.pagos.repository.BilleteraReservaTrackingRepository;
import com.patiperro.pagos.repository.BilleteraSaldoAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Una transacción por liberación ({@link Propagation#REQUIRES_NEW}): evita una TX gigante y permite
 * que un fallo puntual no revierta liberaciones ya válidas. Idempotente si {@code liberado_en} ya está fijado.
 *
 * <p>Bloqueo pesimístico en tracking y billetera para evitar doble liberación concurrente; auditoría opción A
 * referencia el cobro original ({@code id_transaccion_pagos}). No libera si hay disputa activa para la reserva
 * ({@link com.patiperro.pagos.repository.BilleteraDisputaReservaRepository}), coherente con abrir/cerrar disputa
 * que bloquea la misma fila de tracking cuando existe.</p>
 */
@Service
@RequiredArgsConstructor
public class BilleteraLiberacionTransaccionalService {

    private static final Logger log = LoggerFactory.getLogger(BilleteraLiberacionTransaccionalService.class);

    private static final int SCALE = 2;

    private final BilleteraRepository billeteraRepository;
    private final BilleteraReservaTrackingRepository trackingRepository;
    private final BilleteraSaldoAuditoriaRepository billeteraSaldoAuditoriaRepository;
    private final BilleteraDisputaReservaRepository billeteraDisputaReservaRepository;

    /**
     * @return {@code true} si en esta ejecución se persistió la liberación
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean liberarSiPendiente(Long idTracking, ZoneId zone) {
        if (idTracking == null || zone == null) {
            return false;
        }
        BilleteraReservaTracking t = trackingRepository.findByIdTrackingForUpdate(idTracking).orElse(null);
        if (t == null) {
            return false;
        }
        if (t.getLiberadoEn() != null) {
            return false;
        }
        if (t.getFase() != BilleteraReservaFase.EN_VERIFICACION) {
            return false;
        }
        if (t.getFechaFinServicio() == null) {
            return false;
        }
        if (t.getIdTransaccionPagos() == null) {
            log.warn("Billetera liberación omitida: sin id_transaccion_pagos (tracking={})", idTracking);
            return false;
        }
        LocalDate hoy = LocalDate.now(zone);
        LocalDate diaFin = t.getFechaFinServicio().atZone(zone).toLocalDate();
        LocalDate disponibleDesde = diaFin.plusDays(2);
        if (hoy.isBefore(disponibleDesde)) {
            return false;
        }

        if (billeteraDisputaReservaRepository.existsByIdReservaAndDisputaActivaTrue(t.getIdReserva())) {
            log.info("Billetera liberación omitida: disputa activa (reserva={})", t.getIdReserva());
            return false;
        }

        BigDecimal neto = nz(t.getMontoNeto());
        Billetera b = obtenerOBloquearBilletera(t.getIdUsuarioPaseador());

        BigDecimal verAntes = nz(b.getSaldoVerificacion());
        BigDecimal dispAntes = nz(b.getSaldoActual());
        BigDecimal ver = verAntes.subtract(neto).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal disp = dispAntes.add(neto).setScale(SCALE, RoundingMode.HALF_UP);
        if (ver.signum() < 0) {
            log.warn("Billetera liberación: saldo_verificacion negativo (reserva={}); se fija a cero", t.getIdReserva());
            ver = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }

        b.setSaldoVerificacion(ver);
        b.setSaldoActual(disp);
        billeteraRepository.save(b);

        LocalDateTime marca = LocalDateTime.now(zone);
        t.setLiberadoEn(marca);
        trackingRepository.save(t);

        billeteraSaldoAuditoriaRepository.save(BilleteraSaldoAuditoria.builder()
                .idTracking(t.getIdTracking())
                .idReserva(t.getIdReserva())
                .idUsuarioPaseador(t.getIdUsuarioPaseador())
                .idTransaccion(t.getIdTransaccionPagos())
                .montoNeto(neto)
                .saldoVerificacionAntes(verAntes)
                .saldoActualAntes(dispAntes)
                .saldoVerificacionDespues(ver)
                .saldoActualDespues(disp)
                .creadoEn(marca)
                .build());

        log.info("Billetera: liberado a disponible reserva={} neto={}", t.getIdReserva(), neto);
        return true;
    }

    /**
     * Garantiza fila {@link Billetera} y la bloquea para actualización ({@code FOR UPDATE}).
     */
    private Billetera obtenerOBloquearBilletera(Long idUsuarioPaseador) {
        return billeteraRepository
                .findByIdUsuarioForUpdate(idUsuarioPaseador)
                .orElseGet(() -> {
                    try {
                        billeteraRepository.saveAndFlush(Billetera.builder()
                                .idUsuario(idUsuarioPaseador)
                                .saldoActual(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP))
                                .saldoRetenido(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP))
                                .saldoVerificacion(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP))
                                .build());
                    } catch (DataIntegrityViolationException ignored) {
                        // Otro hilo creó la fila.
                    }
                    return billeteraRepository
                            .findByIdUsuarioForUpdate(idUsuarioPaseador)
                            .orElseThrow(() -> new IllegalStateException(
                                    "No se pudo obtener billetera bloqueada para usuario " + idUsuarioPaseador));
                });
    }

    private static BigDecimal nz(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return v.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
