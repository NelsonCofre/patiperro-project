package com.patiperro.pagos.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patiperro.pagos.dto.comprobante.ComprobantePagoResponse;
import com.patiperro.pagos.model.ComprobantePago;
import com.patiperro.pagos.model.EstadoPago;
import com.patiperro.pagos.model.PagoExterno;
import com.patiperro.pagos.model.Transaccion;
import com.patiperro.pagos.repository.ComprobantePagoRepository;
import com.patiperro.pagos.repository.PagoExternoRepository;
import com.patiperro.pagos.repository.TransaccionRepository;
import com.patiperro.pagos.reserva.ReservaConsultaClient;
import com.patiperro.pagos.reserva.dto.ReservaComprobanteDto;
import com.patiperro.pagos.support.NotificacionResumenPagoIntegracionClient;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
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
    private final ComprobantePagoRepository comprobantePagoRepository;
    private final ObjectMapper objectMapper;
    private final ComprobantePagoHtmlRenderer comprobantePagoHtmlRenderer;
    private final NotificacionResumenPagoIntegracionClient notificacionResumenPagoIntegracionClient;

    public ComprobantePagoService(
            ReservaConsultaClient reservaConsultaClient,
            TransaccionRepository transaccionRepository,
            PagoExternoRepository pagoExternoRepository,
            ComprobantePagoRepository comprobantePagoRepository,
            ObjectMapper objectMapper,
            ComprobantePagoHtmlRenderer comprobantePagoHtmlRenderer,
            NotificacionResumenPagoIntegracionClient notificacionResumenPagoIntegracionClient) {
        this.reservaConsultaClient = reservaConsultaClient;
        this.transaccionRepository = transaccionRepository;
        this.pagoExternoRepository = pagoExternoRepository;
        this.comprobantePagoRepository = comprobantePagoRepository;
        this.objectMapper = objectMapper;
        this.comprobantePagoHtmlRenderer = comprobantePagoHtmlRenderer;
        this.notificacionResumenPagoIntegracionClient = notificacionResumenPagoIntegracionClient;
    }

    public ComprobantePagoResponse obtenerParaTutor(Long idReserva, Authentication authentication) {
        Long tutorId = extraerTutorId(authentication);
        if (tutorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        // Intentar devolver snapshot persistido primero (mejor latencia y “histórico” real).
        ComprobantePago persistido = comprobantePagoRepository.findByIdReserva(idReserva).orElse(null);
        if (persistido != null) {
            // Anti-IDOR: si el snapshot tiene owner, validar sin red. Si no lo tiene (migración antigua),
            // caemos al chequeo remoto para seguir “fail-closed”.
            if (persistido.getIdTutorUsuario() != null) {
                if (!tutorId.equals(persistido.getIdTutorUsuario())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
                }
            } else {
                ReservaComprobanteDto r = reservaConsultaClient.obtenerComprobanteInterno(idReserva);
                if (r == null || r.idTutorUsuario() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo obtener datos de reserva");
                }
                if (!tutorId.equals(r.idTutorUsuario().longValue())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
                }
            }
            // Integridad: snapshot ligado a id_transaccion_pagos debe seguir siendo esa tx APROBADA para esta reserva.
            try {
                if (persistido.getIdTransaccionPagos() != null) {
                    validarTransaccionPersistidaExacta(idReserva, persistido.getIdTransaccionPagos());
                } else {
                    validarPagoAprobado(idReserva, null);
                }
            } catch (IllegalStateException ex) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
            }
            return mapearPersistido(persistido);
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

        ComprobantePagoResponse resp = new ComprobantePagoResponse(
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
        persistirSiFalta(resp, tutorId);
        return resp;
    }

    /**
     * Misma autorización y reglas que {@link #obtenerParaTutor}: prioriza HTML persistido para histórico estable.
     */
    public String obtenerHtmlParaTutor(Long idReserva, Authentication authentication) {
        ComprobantePagoResponse dto = obtenerParaTutor(idReserva, authentication);
        ComprobantePago entity = comprobantePagoRepository.findByIdReserva(idReserva).orElse(null);
        if (entity != null && StringUtils.hasText(entity.getHtmlResumen())) {
            return entity.getHtmlResumen();
        }
        try {
            return comprobantePagoHtmlRenderer.render(dto);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el HTML del comprobante");
        }
    }

    /**
     * Flujo interno: genera (si falta) el comprobante para una reserva con pago APROBADO,
     * lo persiste y opcionalmente dispara envío de correo al tutor (best-effort y deduplicado).
     *
     * @param idReserva          reserva objetivo (obligatoria)
     * @param forceEnviarCorreo  si true, intenta envío aun si ya estaba marcado como enviado
     * @return comprobante resultante
     */
    public ComprobantePagoResponse generarPersistirYEnviarResumenTutor(Long idReserva, boolean forceEnviarCorreo) {
        if (idReserva == null) {
            throw new IllegalArgumentException("idReserva es obligatorio");
        }

        // 1) Si existe persistido, validar que haya pago aprobado vigente y luego decidir envío.
        ComprobantePago existente = comprobantePagoRepository.findByIdReserva(idReserva).orElse(null);
        if (existente != null) {
            ComprobantePagoResponse resp = mapearPersistido(existente);
            // Seguridad/integridad: confirmar que hay pago aprobado (evita “comprobante” sin pago).
            validarPagoAprobado(idReserva, resp.idOrden());
            if (forceEnviarCorreo || existente.getEmailEnviadoEn() == null) {
                enviarCorreoTutorBestEffort(idReserva, existente, resp, forceEnviarCorreo);
            }
            return resp;
        }

        // 2) No existe: construir a partir de integraciones internas (reserva-service + tx/pago externo).
        ReservaComprobanteDto r = reservaConsultaClient.obtenerComprobanteInterno(idReserva);
        if (r == null || r.idTutorUsuario() == null) {
            throw new IllegalStateException("No se pudo obtener datos de reserva");
        }

        Transaccion tx = resolverTransaccionAprobada(idReserva, r.idTransaccionPagos());
        if (tx == null || tx.getEstadoPago() != EstadoPago.APROBADO) {
            throw new IllegalStateException("No existe pago aprobado para esta reserva");
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

        ComprobantePagoResponse resp = new ComprobantePagoResponse(
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

        // Persistir con owner (idTutorUsuario) y HTML snapshot.
        persistirSiFalta(resp, r.idTutorUsuario().longValue());

        // Releer para marcar envío si corresponde (y evitar duplicados en carreras).
        ComprobantePago guardado = comprobantePagoRepository.findByIdReserva(idReserva).orElse(null);
        if (guardado != null) {
            enviarCorreoTutorBestEffort(idReserva, guardado, resp, forceEnviarCorreo);
        }
        return resp;
    }

    private void persistirSiFalta(ComprobantePagoResponse resp, Long idTutorUsuario) {
        if (resp == null || resp.idReserva() == null) {
            return;
        }
        Long idReserva = resp.idReserva();
        if (comprobantePagoRepository.findByIdReserva(idReserva).isPresent()) {
            return;
        }
        ComprobantePago e = new ComprobantePago();
        e.setIdReserva(idReserva);
        e.setIdTutorUsuario(idTutorUsuario);
        e.setIdTransaccionPagos(resp.idOrden());
        e.setIdTransaccionExterna(resp.idTransaccionExterna());
        e.setFechaHoraOperacion(resp.fechaHoraOperacion());
        e.setPaseadorNombre(resp.paseadorNombre());
        e.setMascotaNombre(resp.mascotaNombre());
        e.setFechaPaseo(resp.fechaPaseo());
        e.setHoraInicio(resp.horaInicio());
        e.setHoraFinal(resp.horaFinal());
        e.setDuracionMinutos(resp.duracionMinutos());
        e.setMoneda(resp.moneda() != null ? resp.moneda() : "CLP");
        e.setMontoTotal(resp.montoTotal());
        e.setComisionApp(resp.comisionApp());
        e.setMontoNeto(resp.montoNeto());
        e.setEstadoFondos(resp.estadoFondos());
        e.setTipoDocumento(resp.tipoDocumento() != null ? resp.tipoDocumento() : "RESUMEN_TRANSACCION");
        e.setDisclaimerLegal(resp.disclaimerLegal());
        e.setCreadoEn(LocalDateTime.now());
        try {
            e.setJsonSnapshot(objectMapper.writeValueAsString(resp));
        } catch (JsonProcessingException ignored) {
            e.setJsonSnapshot(null);
        }
        try {
            e.setHtmlResumen(comprobantePagoHtmlRenderer.render(resp));
        } catch (RuntimeException ignored) {
            e.setHtmlResumen(null);
        }
        try {
            comprobantePagoRepository.saveAndFlush(e);
        } catch (DataIntegrityViolationException ignored) {
            // Idempotencia concurrente: UNIQUE(id_reserva).
        } catch (RuntimeException ignored) {
            // No debe romper la consulta del tutor: persistencia best-effort.
        }
    }

    private void enviarCorreoTutorBestEffort(
            Long idReserva,
            ComprobantePago entity,
            ComprobantePagoResponse resp,
            boolean forceEnviarCorreo
    ) {
        if (entity == null || resp == null) {
            return;
        }
        if (!forceEnviarCorreo && entity.getEmailEnviadoEn() != null) {
            return;
        }

        String emailDestino = null;
        // Preferir correo obtenido desde reserva-service (source of truth).
        try {
            ReservaComprobanteDto r = reservaConsultaClient.obtenerComprobanteInterno(idReserva);
            if (r != null && StringUtils.hasText(r.tutorCorreo())) {
                emailDestino = r.tutorCorreo().trim();
            }
        } catch (RuntimeException ignored) {
            emailDestino = null;
        }
        if (!StringUtils.hasText(emailDestino)) {
            return;
        }
        String html = entity.getHtmlResumen();
        if (!StringUtils.hasText(html)) {
            try {
                html = comprobantePagoHtmlRenderer.render(resp);
            } catch (RuntimeException ignored) {
                html = null;
            }
        }
        if (!StringUtils.hasText(html)) {
            return;
        }

        boolean ok = notificacionResumenPagoIntegracionClient.enviarResumenTutor(
                idReserva,
                entity.getIdTutorUsuario(),
                emailDestino,
                html
        );
        if (!ok) {
            return;
        }
        try {
            entity.setEmailDestino(emailDestino);
            entity.setEmailEnviadoEn(LocalDateTime.now());
            comprobantePagoRepository.save(entity);
        } catch (RuntimeException ignored) {
            // best-effort: no debe romper flujo interno
        }
    }

    /**
     * Cuando el comprobante guardó un id de transacción, no se acepta sustituir por otra tx aprobada (evita datos incoherentes).
     */
    private void validarTransaccionPersistidaExacta(Long idReserva, Long idTransaccionPagos) {
        Transaccion tx = transaccionRepository.findById(idTransaccionPagos).orElse(null);
        if (tx == null || tx.getEstadoPago() != EstadoPago.APROBADO) {
            throw new IllegalStateException("No existe pago aprobado para esta reserva");
        }
        if (!Objects.equals(tx.getIdReserva(), idReserva)) {
            throw new IllegalStateException("La transacción del comprobante no corresponde a esta reserva");
        }
    }

    private void validarPagoAprobado(Long idReserva, Long idOrdenTransaccion) {
        Transaccion tx = resolverTransaccionAprobada(idReserva, idOrdenTransaccion);
        if (tx == null || tx.getEstadoPago() != EstadoPago.APROBADO) {
            throw new IllegalStateException("No existe pago aprobado para esta reserva");
        }
    }

    private Transaccion resolverTransaccionAprobada(Long idReserva, Long idTransaccionPagos) {
        Transaccion tx = null;
        if (idTransaccionPagos != null) {
            tx = transaccionRepository.findById(idTransaccionPagos).orElse(null);
        }
        if (tx == null) {
            tx = transaccionRepository
                    .findFirstByIdReservaAndEstadoPagoOrderByIdTransaccionDesc(idReserva, EstadoPago.APROBADO)
                    .orElse(null);
        }
        return tx;
    }

    private static ComprobantePagoResponse mapearPersistido(ComprobantePago e) {
        return new ComprobantePagoResponse(
                e.getTipoDocumento(),
                e.getDisclaimerLegal(),
                e.getIdReserva(),
                e.getIdTransaccionPagos(),
                e.getIdTransaccionExterna(),
                e.getFechaHoraOperacion(),
                e.getPaseadorNombre(),
                e.getMascotaNombre(),
                e.getFechaPaseo(),
                e.getHoraInicio(),
                e.getHoraFinal(),
                e.getDuracionMinutos(),
                e.getMoneda(),
                e.getMontoTotal(),
                e.getComisionApp(),
                e.getMontoNeto(),
                e.getEstadoFondos()
        );
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

