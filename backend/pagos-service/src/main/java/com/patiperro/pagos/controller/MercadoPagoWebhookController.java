package com.patiperro.pagos.controller;

import com.patiperro.pagos.service.MercadoPagoWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Recibe notificaciones IPN / webhook de Mercado Pago (Checkout Pro u otros).
 * <p>Mercado Pago puede enviar {@code GET} con {@code topic} e {@code id}, o {@code POST}
 * con cuerpo JSON según la configuración del túnel / versión de API.</p>
 * <p>Responde {@code 200} de inmediato; la confirmación contra la API de Mercado Pago y la
 * actualización en {@code reserva-service} las ejecuta {@link MercadoPagoWebhookService}
 * de forma asíncrona.</p>
 */
@RestController
@RequestMapping("/api/pagos/webhooks/mercadopago")
public class MercadoPagoWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    private final MercadoPagoWebhookService webhookService;

    public MercadoPagoWebhookController(MercadoPagoWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping
    public ResponseEntity<Void> recibirPorQuery(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String data_id) {
        String resourceId = firstNonBlank(id, data_id);
        log.info("Mercado Pago webhook GET topic={} resourceId={}", topic, resourceId);
        webhookService.procesarNotificacion(topic, resourceId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> recibirJson(@RequestBody(required = false) Map<String, Object> body) {
        String topic = extractTopic(body);
        String resourceId = extractResourceId(body);
        int keyCount = body != null ? body.size() : 0;
        log.info("Mercado Pago webhook POST topic={} resourceId={} rawKeys={}", topic, resourceId, keyCount);
        webhookService.procesarNotificacion(topic, resourceId);
        return ResponseEntity.ok().build();
    }

    /**
     * Algunos proxies envían POST sin Content-Type o como formulario.
     */
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> recibirForm(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String data_id,
            @RequestParam(required = false) String type) {
        String resourceId = firstNonBlank(id, data_id);
        String effectiveTopic = firstNonBlank(topic, type);
        log.info("Mercado Pago webhook POST(form) topic={} resourceId={}", effectiveTopic, resourceId);
        webhookService.procesarNotificacion(effectiveTopic, resourceId);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<Void> recibirPostSinTipo(@RequestBody(required = false) Map<String, Object> body) {
        return recibirJson(body);
    }

    private static String extractTopic(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        return firstNonBlank(
                stringVal(body.get("topic")),
                stringVal(body.get("type")),
                stringVal(body.get("action")));
    }

    private static String extractResourceId(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        String direct = firstNonBlank(stringVal(body.get("id")), stringVal(body.get("resource")));
        if (direct != null) {
            return direct;
        }
        Object data = body.get("data");
        if (data instanceof Map<?, ?> map) {
            return firstNonBlank(stringVal(map.get("id")), stringVal(map.get("resource_id")));
        }
        return null;
    }

    private static String stringVal(Object o) {
        if (o == null) {
            return null;
        }
        String v = o.toString().trim();
        return v.isEmpty() ? null : v;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }
}
