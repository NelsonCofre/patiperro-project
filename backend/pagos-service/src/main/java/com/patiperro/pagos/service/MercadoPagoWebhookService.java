package com.patiperro.pagos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Punto de entrada del webhook: validación mínima y delegación al procesador asíncrono.
 */
@Service
public class MercadoPagoWebhookService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookService.class);

    private final MercadoPagoWebhookProcessor processor;

    public MercadoPagoWebhookService(MercadoPagoWebhookProcessor processor) {
        this.processor = processor;
    }

    public void procesarNotificacion(String topic, String resourceId) {
        if (!StringUtils.hasText(resourceId)) {
            log.debug("Webhook MP omitido: sin resourceId (topic={})", topic);
            return;
        }
        processor.procesar(topic, resourceId.trim());
    }
}
