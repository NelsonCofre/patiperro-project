package com.patiperro.pagos.service;

import com.patiperro.pagos.dto.MercadoPagoPaymentDto;
import com.patiperro.pagos.reserva.ReservaConsultaClient;
import com.patiperro.pagos.reserva.dto.ReservaConsultaDto;
import com.patiperro.pagos.support.MercadoPagoApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MercadoPagoCheckoutSincronizacionService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoCheckoutSincronizacionService.class);

    private final MercadoPagoApiClient mercadoPagoApiClient;
    private final ReservaConsultaClient reservaConsultaClient;
    private final MercadoPagoWebhookProcessor mercadoPagoWebhookProcessor;

    public MercadoPagoCheckoutSincronizacionService(
            MercadoPagoApiClient mercadoPagoApiClient,
            ReservaConsultaClient reservaConsultaClient,
            MercadoPagoWebhookProcessor mercadoPagoWebhookProcessor) {
        this.mercadoPagoApiClient = mercadoPagoApiClient;
        this.reservaConsultaClient = reservaConsultaClient;
        this.mercadoPagoWebhookProcessor = mercadoPagoWebhookProcessor;
    }

    /**
     * Valida titularidad del tutor con reserva-service y aplica la misma lógica que el webhook de MP.
     */
    public void sincronizarPagoTutor(Long tutorUsuarioId, String paymentId) {
        if (tutorUsuarioId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no identificado");
        }
        if (!StringUtils.hasText(paymentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentId es obligatorio");
        }
        String pid = paymentId.trim();
        MercadoPagoPaymentDto pago = mercadoPagoApiClient
                .obtenerPago(pid)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.BAD_GATEWAY, "No se pudo consultar el pago en Mercado Pago"));

        Integer idReserva = MercadoPagoWebhookProcessor.resolverIdReserva(pago);
        if (idReserva == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El pago no contiene referencia de reserva reconocible");
        }

        try {
            ReservaConsultaDto r = reservaConsultaClient.obtenerReservaParaPago(idReserva.longValue());
            if (r.idTutorUsuario() == null || !r.idTutorUsuario().equals(tutorUsuarioId)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "El pago no corresponde a las reservas de este tutor");
            }
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

        try {
            mercadoPagoWebhookProcessor.procesarSincrono(pid);
        } catch (RuntimeException e) {
            log.error("Sincronización MP: fallo al aplicar pago (paymentId={})", pid, e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo aplicar el pago en Patiperro");
        }
    }
}
