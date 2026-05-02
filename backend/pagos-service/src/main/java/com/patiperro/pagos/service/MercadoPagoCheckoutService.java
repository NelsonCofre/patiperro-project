package com.patiperro.pagos.service;

import com.patiperro.pagos.checkout.MercadoPagoReservaExternalReference;
import com.patiperro.pagos.config.MercadoPagoCheckoutProperties;
import com.patiperro.pagos.dto.MercadoPagoPreferenceResponseDto;
import com.patiperro.pagos.support.MercadoPagoApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orquesta la creación de preferencias Checkout Pro ({@code POST /checkout/preferences}).
 */
@Service
public class MercadoPagoCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoCheckoutService.class);

    private static final String MONEDA_CLP = "CLP";

    private final MercadoPagoApiClient mercadoPagoApiClient;
    private final MercadoPagoCheckoutProperties checkoutProperties;

    public MercadoPagoCheckoutService(
            MercadoPagoApiClient mercadoPagoApiClient,
            MercadoPagoCheckoutProperties checkoutProperties) {
        this.mercadoPagoApiClient = mercadoPagoApiClient;
        this.checkoutProperties = checkoutProperties;
    }

    /**
     * Crea una preferencia de pago para una reserva. {@code external_reference} usa el prefijo
     * {@code patiperro-reserva:} + id de reserva (parseable por el webhook).
     *
     * @param idReserva      obligatorio; se envía como {@code external_reference}
     * @param montoTotal     monto en CLP (entero)
     * @param tituloItem     título del ítem en el checkout
     * @param idempotencyKey opcional; recomendado para reintentos del mismo intento de pago
     */
    public Optional<PreferenciaCheckoutCreada> crearPreferenciaReserva(
            Integer idReserva,
            BigDecimal montoTotal,
            String tituloItem,
            String idempotencyKey) {
        if (idReserva == null) {
            log.warn("Checkout MP: idReserva es obligatorio");
            return Optional.empty();
        }
        if (montoTotal == null || montoTotal.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Checkout MP: montoTotal inválido (reserva={})", idReserva);
            return Optional.empty();
        }
        if (!checkoutProperties.tieneBackUrlsCompletas()) {
            log.warn("Checkout MP: faltan patiperro.mercadopago.checkout.back-url-* (success, failure, pending)");
            return Optional.empty();
        }

        String titulo = StringUtils.hasText(tituloItem) ? tituloItem.trim() : "Reserva Patiperro";
        BigDecimal enteroClp = montoTotal.setScale(0, RoundingMode.HALF_UP);
        if (enteroClp.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
            log.warn("Checkout MP: monto fuera de rango (reserva={})", idReserva);
            return Optional.empty();
        }
        long precioUnitario = enteroClp.longValue();
        if (precioUnitario <= 0) {
            log.warn("Checkout MP: monto CLP debe ser positivo (reserva={})", idReserva);
            return Optional.empty();
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", titulo);
        item.put("quantity", 1);
        item.put("currency_id", MONEDA_CLP);
        item.put("unit_price", precioUnitario);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", List.of(item));
        body.put("external_reference", MercadoPagoReservaExternalReference.fromReservaId(idReserva));

        Map<String, String> backUrls = new LinkedHashMap<>();
        backUrls.put("success", checkoutProperties.getBackUrlSuccess());
        backUrls.put("failure", checkoutProperties.getBackUrlFailure());
        backUrls.put("pending", checkoutProperties.getBackUrlPending());
        body.put("back_urls", backUrls);
        body.put("auto_return", checkoutProperties.getAutoReturn());

        if (StringUtils.hasText(checkoutProperties.getNotificationUrl())) {
            body.put("notification_url", checkoutProperties.getNotificationUrl());
        }

        Optional<MercadoPagoPreferenceResponseDto> pref =
                mercadoPagoApiClient.crearPreferenciaCheckout(body, idempotencyKey);
        if (pref.isEmpty()) {
            return Optional.empty();
        }
        MercadoPagoPreferenceResponseDto dto = pref.get();
        return Optional.of(new PreferenciaCheckoutCreada(
                dto.id(),
                dto.initPoint(),
                dto.sandboxInitPoint()
        ));
    }

    /**
     * Resultado mínimo para redirigir al tutor al checkout (prod o sandbox).
     */
    public record PreferenciaCheckoutCreada(
            String preferenceId,
            String initPoint,
            String sandboxInitPoint
    ) {
    }
}
