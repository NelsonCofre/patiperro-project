package com.patiperro.pagos.service;

import com.patiperro.pagos.dto.comprobante.ComprobantePagoResponse;
import com.patiperro.pagos.model.ComprobantePagoRegistro;
import com.patiperro.pagos.model.EstadoPago;
import com.patiperro.pagos.model.PagoExterno;
import com.patiperro.pagos.model.Transaccion;
import com.patiperro.pagos.repository.ComprobantePagoRegistroRepository;
import com.patiperro.pagos.repository.PagoExternoRepository;
import com.patiperro.pagos.repository.TransaccionRepository;
import com.patiperro.pagos.reserva.ReservaConsultaClient;
import com.patiperro.pagos.reserva.dto.ReservaComprobanteDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ComprobantePagoService {

    private static final String ESTADO_FONDOS_TEXTO =
            "Pago confirmado. Fondos retenidos en garantía por Patiperro hasta la finalización del servicio";

    private static final String DISCLAIMER =
            "Resumen de Transacción (informativo). No constituye boleta o factura legal.";

    private final ReservaConsultaClient reservaConsultaClient;
    private final TransaccionRepository transaccionRepository;
    private final PagoExternoRepository pagoExternoRepository;
    private final ComprobantePagoRegistroRepository comprobantePagoRegistroRepository;

    public ComprobantePagoService(
            ReservaConsultaClient reservaConsultaClient,
            TransaccionRepository transaccionRepository,
            PagoExternoRepository pagoExternoRepository,
            ComprobantePagoRegistroRepository comprobantePagoRegistroRepository) {
        this.reservaConsultaClient = reservaConsultaClient;
        this.transaccionRepository = transaccionRepository;
        this.pagoExternoRepository = pagoExternoRepository;
        this.comprobantePagoRegistroRepository = comprobantePagoRegistroRepository;
    }

    @Transactional
    public ComprobantePagoResponse obtenerParaTutor(Long idReserva, Authentication authentication) {
        Long tutorId = extraerTutorId(authentication);
        if (tutorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        ReservaComprobanteDto r = reservaConsultaClient.obtenerComprobanteInterno(idReserva);
        if (r == null || r.idTutorUsuario() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo obtener datos de reserva");
        }

        if (!tutorId.equals(r.idTutorUsuario().longValue())) {
            // Anti-IDOR: no permitir que un tutor consulte reservas de terceros.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }

        // Preferir idTransaccionPagos si viene, si no buscar por reserva.
        Transaccion tx = null;
        if (r.idTransaccionPagos() != null) {
            Optional<Transaccion> opt = transaccionRepository.findById(r.idTransaccionPagos());
            tx = opt.orElse(null);
        }
        if (tx == null) {
            tx = transaccionRepository
                    .findFirstByIdReservaAndEstadoPagoOrderByIdTransaccionDesc(idReserva, EstadoPago.APROBADO)
                    .orElse(null);
        }
        if (tx == null || tx.getEstadoPago() != EstadoPago.APROBADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No existe pago aprobado para esta reserva");
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

        ComprobantePagoResponse respuesta = new ComprobantePagoResponse(
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
                "Estado: " + ESTADO_FONDOS_TEXTO
        );

        int idReservaInt = Math.toIntExact(idReserva);
        if (comprobantePagoRegistroRepository.findByIdReserva(idReservaInt).isEmpty()) {
            comprobantePagoRegistroRepository.save(
                    ComprobantePagoRegistro.builder()
                            .idReserva(idReservaInt)
                            .idTransaccionPagos(tx.getIdTransaccion())
                            .providerPaymentId(idExterna)
                            .creadoEn(LocalDateTime.now())
                            .build());
        }

        return respuesta;
    }

    private static Long extraerTutorId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        boolean esTutor = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_TUTOR"::equals);
        if (!esTutor) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long l) {
            return l;
        }
        if (principal instanceof Number n) {
            return n.longValue();
        }
        if (principal instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}

