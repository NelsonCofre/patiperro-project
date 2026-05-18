package com.patiperro.pagos.controller;



import com.patiperro.pagos.config.MercadoPagoCheckoutProperties;

import com.patiperro.pagos.dto.ApiErrorResponse;

import com.patiperro.pagos.dto.checkout.CrearPreferenciaRequest;

import com.patiperro.pagos.dto.checkout.PreferenciaCheckoutResponse;

import com.patiperro.pagos.service.MercadoPagoCheckoutService;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpStatus;

import org.springframework.http.MediaType;

import org.springframework.http.ResponseEntity;

import org.springframework.util.StringUtils;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestHeader;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;



import java.util.Optional;



/**

 * Creación de preferencias Checkout Pro (servidor a servidor). Protegido con la misma cabecera interna que reembolsos.

 */

@RestController

@RequestMapping("/api/pagos/interno/mercadopago/checkout")

public class MercadoPagoCheckoutController {



    @Value("${patiperro.pagos.interno.secret:}")

    private String internoSecret;



    private final MercadoPagoCheckoutService mercadoPagoCheckoutService;

    private final MercadoPagoCheckoutProperties checkoutProperties;



    public MercadoPagoCheckoutController(

            MercadoPagoCheckoutService mercadoPagoCheckoutService,

            MercadoPagoCheckoutProperties checkoutProperties) {

        this.mercadoPagoCheckoutService = mercadoPagoCheckoutService;

        this.checkoutProperties = checkoutProperties;

    }



    /**

     * Crea una preferencia Mercado Pago Checkout Pro para cobrar una reserva.

     * <p>Cabeceras: {@value InternalMercadoPagoController#HEADER_INTERNO}, opcional {@value InternalMercadoPagoController#HEADER_IDEMPOTENCY_KEY}.</p>

     */

    @PostMapping(value = "/preferencia", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)

    public ResponseEntity<?> crearPreferencia(

            @RequestHeader(value = InternalMercadoPagoController.HEADER_INTERNO, required = false) String secretoHeader,

            @RequestHeader(value = InternalMercadoPagoController.HEADER_IDEMPOTENCY_KEY, required = false) String idempotencyKey,

            @RequestBody(required = false) CrearPreferenciaRequest body) {

        if (!StringUtils.hasText(internoSecret)) {

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiErrorResponse.servicioNoDisponible());

        }

        if (!internoSecret.equals(secretoHeader)) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiErrorResponse.accesoDenegado());

        }

        if (body == null || body.idReserva() == null || body.montoTotal() == null) {

            return ResponseEntity.badRequest()

                    .body(ApiErrorResponse.solicitudInvalida("idReserva y montoTotal son obligatorios."));

        }



        Optional<MercadoPagoCheckoutService.PreferenciaCheckoutCreada> opt = mercadoPagoCheckoutService.crearPreferenciaReserva(

                body.idReserva(),

                body.montoTotal(),

                body.tituloItem(),

                idempotencyKey);

        if (opt.isEmpty()) {

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiErrorResponse.checkoutNoDisponible());

        }

        return ResponseEntity.ok(toResponse(opt.get()));

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

}


