package com.patiperro.pagos.controller;

import com.patiperro.pagos.dto.disputa.DisputaReservaResponse;
import com.patiperro.pagos.service.BilleteraDisputaReservaService;
import com.patiperro.pagos.service.BilleteraService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/pagos/interno/billetera")
public class InternalBilleteraController {

    @Value("${patiperro.pagos.interno.secret:}")
    private String internoSecret;

    private final BilleteraService billeteraService;
    private final BilleteraDisputaReservaService billeteraDisputaReservaService;

    public InternalBilleteraController(
            BilleteraService billeteraService, BilleteraDisputaReservaService billeteraDisputaReservaService) {
        this.billeteraService = billeteraService;
        this.billeteraDisputaReservaService = billeteraDisputaReservaService;
    }

    @PostMapping("/acreditar-retenido")
    public ResponseEntity<Void> acreditarRetenido(
            @RequestHeader(value = InternalMercadoPagoController.HEADER_INTERNO, required = false) String secretoHeader,
            @RequestBody(required = false) AcreditarRetenidoBody body) {
        ResponseEntity<Void> rechazo = validarSecretoResponse(secretoHeader);
        if (rechazo != null) {
            return rechazo;
        }
        if (body == null || body.idReserva() == null || body.idUsuarioPaseador() == null || body.idTransaccionPagos() == null) {
            return ResponseEntity.badRequest().build();
        }
        billeteraService.acreditarRetenido(body.idReserva(), body.idUsuarioPaseador(), body.idTransaccionPagos());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/pasar-verificacion")
    public ResponseEntity<Void> pasarVerificacion(
            @RequestHeader(value = InternalMercadoPagoController.HEADER_INTERNO, required = false) String secretoHeader,
            @RequestBody(required = false) PasarVerificacionBody body) {
        ResponseEntity<Void> rechazo = validarSecretoResponse(secretoHeader);
        if (rechazo != null) {
            return rechazo;
        }
        if (body == null || body.idReserva() == null || body.idUsuarioPaseador() == null) {
            return ResponseEntity.badRequest().build();
        }
        billeteraService.pasarAVerificacion(body.idReserva(), body.idUsuarioPaseador(), body.fechaFinServicio());
        return ResponseEntity.noContent().build();
    }

    /** Idéntico contrato para reembolsos; delega en {@link BilleteraService#revertirRetenido} (retenido o verificación). */
    @PostMapping("/revertir-retenido")
    public ResponseEntity<Void> revertirRetenido(
            @RequestHeader(value = InternalMercadoPagoController.HEADER_INTERNO, required = false) String secretoHeader,
            @RequestBody(required = false) RevertirRetenidoBody body) {
        ResponseEntity<Void> rechazo = validarSecretoResponse(secretoHeader);
        if (rechazo != null) {
            return rechazo;
        }
        if (body == null || body.idReserva() == null) {
            return ResponseEntity.badRequest().build();
        }
        billeteraService.revertirRetenido(body.idReserva());
        return ResponseEntity.noContent().build();
    }

    /**
     * Abre disputa para una reserva (bloquea liberación a disponible). Misma cabecera secreta que el resto de internos.
     */
    @PostMapping("/disputa/abrir")
    public ResponseEntity<DisputaReservaResponse> abrirDisputa(
            @RequestHeader(value = InternalMercadoPagoController.HEADER_INTERNO, required = false) String secretoHeader,
            @RequestBody(required = false) AbrirDisputaBody body) {
        ResponseEntity<Void> rechazo = validarSecretoResponse(secretoHeader);
        if (rechazo != null) {
            return ResponseEntity.status(rechazo.getStatusCode()).body(null);
        }
        if (body == null || body.idReserva() == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(billeteraDisputaReservaService.abrirDisputa(body.idReserva(), body.motivo()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /** Cierra disputa (idempotente). */
    @PostMapping("/disputa/cerrar")
    public ResponseEntity<Void> cerrarDisputa(
            @RequestHeader(value = InternalMercadoPagoController.HEADER_INTERNO, required = false) String secretoHeader,
            @RequestBody(required = false) CerrarDisputaBody body) {
        ResponseEntity<Void> rechazo = validarSecretoResponse(secretoHeader);
        if (rechazo != null) {
            return rechazo;
        }
        if (body == null || body.idReserva() == null) {
            return ResponseEntity.badRequest().build();
        }
        billeteraDisputaReservaService.cerrarDisputa(body.idReserva());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Void> validarSecretoResponse(String header) {
        if (!StringUtils.hasText(internoSecret)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!internoSecret.equals(header)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return null;
    }

    public record AcreditarRetenidoBody(Integer idReserva, Long idUsuarioPaseador, Long idTransaccionPagos) {
    }

    public record PasarVerificacionBody(Integer idReserva, Long idUsuarioPaseador, LocalDateTime fechaFinServicio) {
    }

    public record RevertirRetenidoBody(Integer idReserva) {
    }

    public record AbrirDisputaBody(Integer idReserva, String motivo) {}

    public record CerrarDisputaBody(Integer idReserva) {}
}
