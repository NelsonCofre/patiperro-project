package com.patiperro.pagos.controller;

import com.patiperro.pagos.service.ReembolsoReservaInternoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pagos/interno/reembolso")
public class InternalReembolsoReservaController {

    public static final String HEADER_INTERNO = InternalMercadoPagoController.HEADER_INTERNO;

    private final ReembolsoReservaInternoService reembolsoReservaInternoService;

    @Value("${patiperro.pagos.interno.secret:}")
    private String internoSecret;

    public InternalReembolsoReservaController(ReembolsoReservaInternoService reembolsoReservaInternoService) {
        this.reembolsoReservaInternoService = reembolsoReservaInternoService;
    }

    @GetMapping("/flags-reserva/{idReserva}")
    public ResponseEntity<ReembolsoReservaInternoService.ReembolsoFlags> flagsReserva(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @PathVariable("idReserva") Integer idReserva) {
        if (!StringUtils.hasText(internoSecret)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!internoSecret.equals(secretoHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(reembolsoReservaInternoService.consultarFlags(idReserva));
    }

    @GetMapping("/candidatos-correo")
    public ResponseEntity<List<Integer>> candidatosCorreo(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @RequestParam(name = "size", defaultValue = "30") int size) {
        if (!StringUtils.hasText(internoSecret)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!internoSecret.equals(secretoHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        int lim = Math.min(Math.max(size, 1), 200);
        return ResponseEntity.ok(reembolsoReservaInternoService.listarCandidatosCorreoReembolso(PageRequest.of(0, lim)));
    }

    @PostMapping("/marcar-correo-reembolso-enviado")
    public ResponseEntity<Void> marcarCorreo(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @RequestBody(required = false) IdReservaBody body) {
        if (!StringUtils.hasText(internoSecret)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!internoSecret.equals(secretoHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (body == null || body.idReserva() == null) {
            return ResponseEntity.badRequest().build();
        }
        reembolsoReservaInternoService.marcarCorreoReembolsoEnviado(body.idReserva());
        return ResponseEntity.noContent().build();
    }

    public record IdReservaBody(Integer idReserva) {
    }
}
