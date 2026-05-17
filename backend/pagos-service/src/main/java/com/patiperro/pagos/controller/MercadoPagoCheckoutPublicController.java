package com.patiperro.pagos.controller;

import com.patiperro.pagos.config.MercadoPagoCheckoutProperties;
import com.patiperro.pagos.dto.ApiErrorResponse;
import com.patiperro.pagos.dto.checkout.CrearPreferenciaRequest;
import com.patiperro.pagos.dto.checkout.PreferenciaCheckoutResponse;
import com.patiperro.pagos.dto.checkout.SincronizarPagoCheckoutRequest;
import com.patiperro.pagos.service.MercadoPagoCheckoutService;
import com.patiperro.pagos.service.MercadoPagoCheckoutSincronizacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * Endpoint autenticado para que el frontend tutor cree preferencias Checkout Pro sin usar cabecera interna.
 */
@RestController
@RequestMapping("/api/pagos/checkout/pro")
public class MercadoPagoCheckoutPublicController {

    private final MercadoPagoCheckoutService mercadoPagoCheckoutService;
    private final MercadoPagoCheckoutProperties checkoutProperties;
    private final MercadoPagoCheckoutSincronizacionService mercadoPagoCheckoutSincronizacionService;

    public MercadoPagoCheckoutPublicController(
            MercadoPagoCheckoutService mercadoPagoCheckoutService,
            MercadoPagoCheckoutProperties checkoutProperties,
            MercadoPagoCheckoutSincronizacionService mercadoPagoCheckoutSincronizacionService) {
        this.mercadoPagoCheckoutService = mercadoPagoCheckoutService;
        this.checkoutProperties = checkoutProperties;
        this.mercadoPagoCheckoutSincronizacionService = mercadoPagoCheckoutSincronizacionService;
    }

    @PostMapping(value = "/preferencia", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> crearPreferenciaTutor(
            @RequestHeader(value = InternalMercadoPagoController.HEADER_IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            @RequestBody(required = false) CrearPreferenciaRequest body) {
        Long tutorId = extraerTutorId(SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null);
        if (tutorId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiErrorResponse.accesoDenegado());
        }

        if (body == null || body.idReserva() == null || body.montoTotal() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.solicitudInvalida("idReserva y montoTotal son obligatorios."));
        }

        Optional<MercadoPagoCheckoutService.PreferenciaCheckoutCreada> opt = mercadoPagoCheckoutService.crearPreferenciaReserva(
                body.idReserva(),
                body.montoTotal(),
                body.tituloItem(),
                idempotencyKey
        );
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiErrorResponse.checkoutNoDisponible());
        }
        return ResponseEntity.ok(toResponse(opt.get()));
    }

    /**
     * Tras volver de Checkout Pro (p. ej. local sin webhook), el tutor confirma el cobro consultando MP
     * y aplicando la misma lógica que el IPN.
     */
    @PostMapping(value = "/sincronizar-pago", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> sincronizarPagoTutor(@Valid @RequestBody SincronizarPagoCheckoutRequest body) {
        Long tutorId = extraerTutorId(SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null);
        if (tutorId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiErrorResponse.accesoDenegado());
        }
        mercadoPagoCheckoutSincronizacionService.sincronizarPagoTutor(tutorId, body.paymentId());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private PreferenciaCheckoutResponse toResponse(MercadoPagoCheckoutService.PreferenciaCheckoutCreada c) {
        String init = c.initPoint();
        String sandbox = c.sandboxInitPoint();
        String url;
        if (checkoutProperties.isUseSandbox()) {
            url = StringUtils.hasText(sandbox) ? sandbox : init;
        } else {
            url = StringUtils.hasText(init) ? init : sandbox;
        }
        return new PreferenciaCheckoutResponse(c.preferenceId(), init, sandbox, url);
    }

    private static Long extraerTutorId(Object principal) {
        if (principal instanceof Long id) {
            return id;
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
