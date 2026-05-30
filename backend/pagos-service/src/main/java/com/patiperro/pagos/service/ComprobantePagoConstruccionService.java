package com.patiperro.pagos.service;

import com.patiperro.pagos.dto.comprobante.ComprobantePagoResponse;
import com.patiperro.pagos.model.EstadoPago;
import com.patiperro.pagos.model.PagoExterno;
import com.patiperro.pagos.model.Transaccion;
import com.patiperro.pagos.repository.PagoExternoRepository;
import com.patiperro.pagos.repository.TransaccionRepository;
import com.patiperro.pagos.reserva.dto.ReservaComprobanteDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Arma el {@link ComprobantePagoResponse} a partir de datos de reserva + transacción aprobada local.
 * Sin validación JWT (uso interno y tutor vía {@link ComprobantePagoService}).
 */
@Service
public class ComprobantePagoConstruccionService {

    static final String ESTADO_FONDOS_TEXTO =
            "Pago confirmado. Fondos retenidos en garantía por Patiperro hasta la finalización del servicio";

    static final String DISCLAIMER =
            "Resumen de Transacción (informativo). No constituye boleta o factura legal.";

    private final TransaccionRepository transaccionRepository;
    private final PagoExternoRepository pagoExternoRepository;

    public ComprobantePagoConstruccionService(
            TransaccionRepository transaccionRepository,
            PagoExternoRepository pagoExternoRepository) {
        this.transaccionRepository = transaccionRepository;
        this.pagoExternoRepository = pagoExternoRepository;
    }

    /**
     * @param idReserva mismo identificador que {@link ReservaComprobanteDto#idReserva()} (numérico como long)
     */
    public Optional<ComprobantePagoResponse> construirSiHayPagoAprobado(ReservaComprobanteDto r, long idReserva) {
        if (r == null) {
            return Optional.empty();
        }

        Transaccion tx = null;
        if (r.idTransaccionPagos() != null) {
            tx = transaccionRepository.findById(r.idTransaccionPagos()).orElse(null);
        }
        if (tx == null) {
            tx = transaccionRepository
                    .findFirstByIdReservaAndEstadoPagoOrderByIdTransaccionDesc(idReserva, EstadoPago.APROBADO)
                    .orElse(null);
        }
        if (tx == null || tx.getEstadoPago() != EstadoPago.APROBADO) {
            return Optional.empty();
        }

        PagoExterno pe = pagoExternoRepository.findByTransaccion_IdTransaccion(tx.getIdTransaccion()).orElse(null);

        LocalDateTime fechaOperacion = null;
        if (pe != null && pe.getFechaAprobacion() != null) {
            fechaOperacion = pe.getFechaAprobacion();
        } else if (tx.getFechaCreacion() != null) {
            fechaOperacion = tx.getFechaCreacion();
        }

        Long duracionMin = null;
        if (r.horaInicio() != null && r.horaFinal() != null) {
            try {
                duracionMin = Math.max(0, Duration.between(r.horaInicio(), r.horaFinal()).toMinutes());
            } catch (RuntimeException ignored) {
                duracionMin = null;
            }
        }

        String idExterna = null;
        if (pe != null && StringUtils.hasText(pe.getProviderPaymentId())) {
            idExterna = pe.getProviderPaymentId().trim();
        } else if (tx.getIdPago() != null) {
            idExterna = String.valueOf(tx.getIdPago());
        }

        return Optional.of(
                new ComprobantePagoResponse(
                        "RESUMEN_TRANSACCION",
                        DISCLAIMER,
                        idReserva,
                        tx.getIdTransaccion(),
                        idExterna,
                        fechaOperacion,
                        r.paseadorNombre(),
                        r.mascotaNombre(),
                        r.fechaPaseo(),
                        r.horaInicio(),
                        r.horaFinal(),
                        duracionMin,
                        "CLP",
                        tx.getMontoBruto(),
                        tx.getComisionApp(),
                        tx.getMontoNeto(),
                        "Estado: " + ESTADO_FONDOS_TEXTO));
    }
}
