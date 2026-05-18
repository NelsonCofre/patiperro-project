package com.patiperro.pagos.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patiperro.pagos.dto.comprobante.ComprobantePagoResponse;
import com.patiperro.pagos.model.ComprobantePago;
import com.patiperro.pagos.model.ComprobantePagoSnapshotSchema;
import com.patiperro.pagos.model.EstadoPago;
import com.patiperro.pagos.model.Transaccion;
import com.patiperro.pagos.repository.ComprobantePagoRepository;
import com.patiperro.pagos.repository.TransaccionRepository;
import com.patiperro.pagos.reserva.ReservaConsultaClient;
import com.patiperro.pagos.reserva.dto.ReservaComprobanteDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class ComprobantePagoService {

    private static final Logger log = LoggerFactory.getLogger(ComprobantePagoService.class);

    private final ReservaConsultaClient reservaConsultaClient;
    private final ComprobantePagoConstruccionService construccionService;
    private final ComprobantePagoRepository comprobantePagoRepository;
    private final TransaccionRepository transaccionRepository;
    private final ObjectMapper objectMapper;
    private final ComprobantePagoHtmlRenderer comprobantePagoHtmlRenderer;

    public ComprobantePagoService(
            ReservaConsultaClient reservaConsultaClient,
            ComprobantePagoConstruccionService construccionService,
            ComprobantePagoRepository comprobantePagoRepository,
            TransaccionRepository transaccionRepository,
            ObjectMapper objectMapper,
            ComprobantePagoHtmlRenderer comprobantePagoHtmlRenderer) {
        this.reservaConsultaClient = reservaConsultaClient;
        this.construccionService = construccionService;
        this.comprobantePagoRepository = comprobantePagoRepository;
        this.transaccionRepository = transaccionRepository;
        this.objectMapper = objectMapper;
        this.comprobantePagoHtmlRenderer = comprobantePagoHtmlRenderer;
    }

    @Transactional(readOnly = true)
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }

        Optional<ComprobantePagoResponse> desdeSnapshot = leerSnapshotSiCorresponde(idReserva, tutorId,
                r.idTutorUsuario());
        if (desdeSnapshot.isPresent()) {
            return desdeSnapshot.get();
        }

        return construccionService
                .construirSiHayPagoAprobado(r, idReserva)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "No existe pago aprobado para esta reserva"));
    }

    public String obtenerHtmlParaTutor(Long idReserva, Authentication authentication) {
        ComprobantePagoResponse dto = obtenerParaTutor(idReserva, authentication);
        return comprobantePagoHtmlRenderer.render(dto);
    }

    /**
     * Prioriza snapshot persistido (misma tutoría) para historial estable; si falla
     * deserialización o versión,
     * cae al cálculo en vivo.
     */
    private Optional<ComprobantePagoResponse> leerSnapshotSiCorresponde(
            Long idReserva, Long tutorIdJwt, Integer idTutorUsuarioReserva) {
        Optional<ComprobantePago> row = comprobantePagoRepository.findByIdReserva(idReserva);
        if (row.isEmpty()) {
            return Optional.empty();
        }
        ComprobantePago c = row.get();
        if (!tutorIdJwt.equals(c.getIdTutorUsuario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }
        if (idTutorUsuarioReserva != null && !c.getIdTutorUsuario().equals(idTutorUsuarioReserva.longValue())) {
            log.warn(
                    "Comprobante snapshot: id_tutor_usuario en BD no coincide con reserva; se ignora snapshot (idReserva={})",
                    idReserva);
            return Optional.empty();
        }
        Integer ver = c.getSnapshotSchemaVersion();
        if (ver == null || ver != ComprobantePagoSnapshotSchema.CURRENT) {
            log.debug("Comprobante snapshot: versión esquema no actual (idReserva={}, ver={})", idReserva, ver);
            return Optional.empty();
        }
        // Consistencia con el endpoint JSON "en vivo": solo servir comprobante si la
        // transacción sigue APROBADA.
        if (c.getIdTransaccion() != null) {
            Optional<Transaccion> tx = transaccionRepository.findById(c.getIdTransaccion());
            if (tx.isEmpty() || tx.get().getEstadoPago() != EstadoPago.APROBADO) {
                log.debug(
                        "Comprobante snapshot: transacción no aprobada; no se sirve snapshot (idReserva={}, idTx={})",
                        idReserva,
                        c.getIdTransaccion());
                return Optional.empty();
            }
        }
        try {
            return Optional.of(objectMapper.readValue(c.getSnapshotJson(), ComprobantePagoResponse.class));
        } catch (JsonProcessingException e) {
            log.debug("Comprobante snapshot: JSON ilegible, se usa cálculo en vivo (idReserva={})", idReserva, e);
            return Optional.empty();
        }
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
