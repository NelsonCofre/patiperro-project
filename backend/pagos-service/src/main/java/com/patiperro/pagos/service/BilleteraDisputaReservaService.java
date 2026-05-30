package com.patiperro.pagos.service;

import com.patiperro.pagos.dto.disputa.DisputaReservaResponse;
import com.patiperro.pagos.model.BilleteraDisputaReserva;
import com.patiperro.pagos.repository.BilleteraDisputaReservaRepository;
import com.patiperro.pagos.repository.BilleteraReservaTrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Abre/cierra disputas por {@code id_reserva}. Serializa con la liberación a disponible bloqueando la misma fila
 * {@link com.patiperro.pagos.model.BilleteraReservaTracking} cuando existe.
 */
@Service
@RequiredArgsConstructor
public class BilleteraDisputaReservaService {

    private static final int MOTIVO_MAX = 512;

    private final BilleteraDisputaReservaRepository disputaRepository;
    private final BilleteraReservaTrackingRepository trackingRepository;

    /**
     * Idempotente si ya hay disputa activa para la reserva.
     */
    @Transactional
    public DisputaReservaResponse abrirDisputa(Integer idReserva, String motivo) {
        if (idReserva == null) {
            throw new IllegalArgumentException("idReserva es obligatorio");
        }
        trackingRepository.findByIdReservaForUpdate(idReserva);
        String m = truncarMotivo(motivo);
        LocalDateTime ahora = LocalDateTime.now();
        Optional<BilleteraDisputaReserva> existing = disputaRepository.findById(idReserva);
        if (existing.isPresent()) {
            BilleteraDisputaReserva d = existing.get();
            if (d.isDisputaActiva()) {
                return toResponse(d);
            }
            d.setDisputaActiva(true);
            d.setMotivo(m);
            d.setAbiertoEn(ahora);
            d.setCerradoEn(null);
            disputaRepository.save(d);
            return toResponse(d);
        }
        BilleteraDisputaReserva created = BilleteraDisputaReserva.builder()
                .idReserva(idReserva)
                .disputaActiva(true)
                .motivo(m)
                .abiertoEn(ahora)
                .build();
        disputaRepository.save(created);
        return toResponse(created);
    }

    /**
     * Idempotente si no hay fila o la disputa ya estaba cerrada.
     */
    @Transactional
    public void cerrarDisputa(Integer idReserva) {
        if (idReserva == null) {
            return;
        }
        trackingRepository.findByIdReservaForUpdate(idReserva);
        Optional<BilleteraDisputaReserva> existing = disputaRepository.findById(idReserva);
        if (existing.isEmpty() || !existing.get().isDisputaActiva()) {
            return;
        }
        BilleteraDisputaReserva d = existing.get();
        d.setDisputaActiva(false);
        d.setCerradoEn(LocalDateTime.now());
        disputaRepository.save(d);
    }

    private static DisputaReservaResponse toResponse(BilleteraDisputaReserva d) {
        return new DisputaReservaResponse(d.getIdReserva(), d.isDisputaActiva(), d.getAbiertoEn(), d.getCerradoEn());
    }

    private static String truncarMotivo(String motivo) {
        if (motivo == null) {
            return null;
        }
        String t = motivo.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() <= MOTIVO_MAX ? t : t.substring(0, MOTIVO_MAX);
    }
}
