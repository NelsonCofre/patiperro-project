package com.patiperro.pagos.service;

import com.patiperro.pagos.model.Billetera;
import com.patiperro.pagos.model.BilleteraReservaFase;
import com.patiperro.pagos.model.BilleteraReservaTracking;
import com.patiperro.pagos.repository.BilleteraRepository;
import com.patiperro.pagos.repository.BilleteraReservaTrackingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 */
@Service
@RequiredArgsConstructor
public class BilleteraLiberacionTransaccionalService {

    private static final Logger log = LoggerFactory.getLogger(BilleteraLiberacionTransaccionalService.class);

    private static final int SCALE = 2;

    private final BilleteraRepository billeteraRepository;
    private final BilleteraReservaTrackingRepository trackingRepository;

    /**
     * @return {@code true} si en esta ejecución se persistió la liberación
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean liberarSiPendiente(Long idTracking, ZoneId zone) {
        if (idTracking == null || zone == null) {
            return false;
        }
        BilleteraReservaTracking t = trackingRepository.findById(idTracking).orElse(null);
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
        LocalDate hoy = LocalDate.now(zone);
        LocalDate diaFin = t.getFechaFinServicio().atZone(zone).toLocalDate();
        LocalDate disponibleDesde = diaFin.plusDays(2);
        if (hoy.isBefore(disponibleDesde)) {
            return false;
        }

        BigDecimal neto = nz(t.getMontoNeto());
        Billetera b = obtenerOCrearBilletera(t.getIdUsuarioPaseador());
        BigDecimal ver = nz(b.getSaldoVerificacion()).subtract(neto).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal disp = nz(b.getSaldoActual()).add(neto).setScale(SCALE, RoundingMode.HALF_UP);
        if (ver.signum() < 0) {
            log.warn("Billetera liberación: saldo_verificacion negativo (reserva={}); se fija a cero", t.getIdReserva());
            ver = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        b.setSaldoVerificacion(ver);
        b.setSaldoActual(disp);
        billeteraRepository.save(b);

        t.setLiberadoEn(LocalDateTime.now(zone));
        trackingRepository.save(t);
        log.info("Billetera: liberado a disponible reserva={} neto={}", t.getIdReserva(), neto);
        return true;
    }

    private Billetera obtenerOCrearBilletera(Long idUsuarioPaseador) {
        return billeteraRepository.findByIdUsuario(idUsuarioPaseador).orElseGet(() -> billeteraRepository.save(
                Billetera.builder()
                        .idUsuario(idUsuarioPaseador)
                        .saldoActual(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP))
                        .saldoRetenido(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP))
                        .saldoVerificacion(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP))
                        .build()));
    }

    private static BigDecimal nz(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return v.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
