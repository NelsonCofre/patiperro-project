package com.patiperro.pagos.controller;

import com.patiperro.pagos.dto.comprobante.ComprobantePagoResponse;
import com.patiperro.pagos.service.ComprobantePagoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint interno para generar/persistir y enviar por correo el resumen de pago al tutor.
 * Protegido por cabecera secreta (no JWT).
 */
@RestController
@RequestMapping("/api/pagos/interno/comprobante")
public class InternalComprobantePagoController {

    public static final String HEADER_INTERNO = InternalMercadoPagoController.HEADER_INTERNO;

    private final ComprobantePagoService comprobantePagoService;

    @Value("${patiperro.pagos.interno.secret:}")
    private String internoSecret;

    public InternalComprobantePagoController(ComprobantePagoService comprobantePagoService) {
        this.comprobantePagoService = comprobantePagoService;
    }

    @PostMapping("/generar-y-enviar")
    public ResponseEntity<ComprobantePagoResponse> generarYEnviar(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @RequestBody(required = false) GenerarComprobanteRequest body
    ) {
        if (!StringUtils.hasText(internoSecret)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!internoSecret.equals(secretoHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (body == null || body.idReserva() == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            ComprobantePagoResponse resp = comprobantePagoService.generarPersistirYEnviarResumenTutor(
                    body.idReserva(),
                    body.forceEnviarCorreo() != null && body.forceEnviarCorreo()
            );
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    public record GenerarComprobanteRequest(Long idReserva, Boolean forceEnviarCorreo) {
    }
}

