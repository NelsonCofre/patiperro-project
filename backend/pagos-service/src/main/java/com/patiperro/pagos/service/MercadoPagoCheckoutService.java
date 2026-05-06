package com.patiperro.pagos.service;

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
     * Crea una preferencia de pago para una reserva.
     * Se usa formato {@code external_reference = "reserva-" + idReserva} para mantener paridad
     * con el prototipo validado en pago-service.
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
        body.put("external_reference", "reserva-" + idReserva);

        Map<String, String> backUrls = new LinkedHashMap<>();
        backUrls.put("success", checkoutProperties.getBackUrlSuccess());
        backUrls.put("failure", checkoutProperties.getBackUrlFailure());
        backUrls.put("pending", checkoutProperties.getBackUrlPending());
        body.put("back_urls", backUrls);
        String successUrl = checkoutProperties.getBackUrlSuccess();
        boolean successHttps = StringUtils.hasText(successUrl)
                && successUrl.trim().regionMatches(true, 0, "https://", 0, 8);
        if (successHttps && StringUtils.hasText(checkoutProperties.getAutoReturn())) {
            body.put("auto_return", checkoutProperties.getAutoReturn());
        }

        if (StringUtils.hasText(checkoutProperties.getNotificationUrl())) {
            body.put("notification_url", checkoutProperties.getNotificationUrl());
        }

        logCuerpoPreferenciaAntesDePost(idReserva, body);

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

    /**
     * Log diagnóstico antes de {@code POST /checkout/preferences} (equivale a persistir la preferencia en MP).
     * No incluye datos sensibles; {@code payer} solo aparece si lo añadís al map en el futuro.
     */
    @SuppressWarnings("unchecked")
    private void logCuerpoPreferenciaAntesDePost(Integer idReserva, Map<String, Object> body) {
        Object payer = body.get("payer");
        Object ext = body.get("external_reference");
        Object ar = body.get("auto_return");
        Object notif = body.containsKey("notification_url") ? "presente" : "ausente";
        Map<String, String> back = null;
        try {
            back = (Map<String, String>) body.get("back_urls");
        } catch (ClassCastException ignored) {
            // ignorar
        }
        try {
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            if (items != null && !items.isEmpty()) {
                Map<String, Object> it = items.get(0);
                log.info(
                        "Checkout MP (payload pre-POST): reserva={} external_reference={} items[0]: title={} quantity={} "
                                + "unit_price={} currency_id={} back_urls: success={} failure={} pending={} "
                                + "auto_return={} notification_url={} payer={}",
                        idReserva,
                        ext,
                        it.get("title"),
                        it.get("quantity"),
                        it.get("unit_price"),
                        it.get("currency_id"),
                        back != null ? back.get("success") : null,
                        back != null ? back.get("failure") : null,
                        back != null ? back.get("pending") : null,
                        ar,
                        notif,
                        payer != null ? "presente" : "ausente");
                return;
            }
        } catch (ClassCastException e) {
            // caer al log genérico
        }
        log.info("Checkout MP (payload pre-POST): reserva={} keys={} external_reference={}",
                idReserva, body.keySet(), ext);
    }
}
