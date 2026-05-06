package com.patiperro.pagos.service;

import com.patiperro.pagos.dto.MercadoPagoPaymentDto;
import com.patiperro.pagos.model.PagoExterno;
import com.patiperro.pagos.model.Transaccion;
import com.patiperro.pagos.repository.PagoExternoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Persistencia y lógica de idempotencia para pagos externos (p. ej. Mercado Pago).
 *
 * <p>Nota: {@link PagoExterno} requiere una {@link Transaccion} asociada. Este servicio
 * asume que la transacción existe y se entrega desde el flujo de creación de pago.</p>
 */
@Service
@RequiredArgsConstructor
public class PagoExternoService {

    public static final String PROVIDER_MERCADOPAGO = "MERCADOPAGO";

    private final PagoExternoRepository pagoExternoRepository;

    public Optional<PagoExterno> buscarMercadoPagoPorPaymentId(String providerPaymentId) {
        if (!StringUtils.hasText(providerPaymentId)) {
            return Optional.empty();
        }
        return pagoExternoRepository.findByProviderAndProviderPaymentId(PROVIDER_MERCADOPAGO, providerPaymentId.trim());
    }

    /**
     * Upsert de un pago externo de Mercado Pago.
     *
     * @param transaccion transacción del dominio Patiperro (obligatoria)
     * @param pago        payload mínimo del pago obtenido desde API de Mercado Pago (obligatorio)
     * @param rawJson     respuesta cruda (opcional) para trazabilidad
     * @return entidad persistida
     */
    @Transactional
    public PagoExterno upsertMercadoPagoPagoExterno(Transaccion transaccion, MercadoPagoPaymentDto pago, String rawJson) {
        if (transaccion == null) {
            throw new IllegalArgumentException("transaccion es obligatoria");
        }
        if (pago == null) {
            throw new IllegalArgumentException("pago es obligatorio");
        }
        String paymentId = pago.idAsString();
        String externalReference = safe(pago.externalReference());

        PagoExterno existente = null;
        if (StringUtils.hasText(paymentId)) {
            existente = pagoExternoRepository
                    .findByProviderAndProviderPaymentId(PROVIDER_MERCADOPAGO, paymentId.trim())
                    .orElse(null);
        }
        if (existente == null && StringUtils.hasText(externalReference)) {
            existente = pagoExternoRepository
                    .findByProviderAndExternalReference(PROVIDER_MERCADOPAGO, externalReference)
                    .orElse(null);
        }

        PagoExterno e = existente != null ? existente : new PagoExterno();
        e.setTransaccion(transaccion);
        e.setProvider(PROVIDER_MERCADOPAGO);
        e.setProviderPaymentId(paymentId);
        e.setExternalReference(externalReference);
        e.setStatus(safe(pago.status()));
        e.setStatusDetail(safe(pago.statusDetail()));
        if ("approved".equalsIgnoreCase(safe(pago.status())) && e.getFechaAprobacion() == null) {
            e.setFechaAprobacion(LocalDateTime.now());
        }
        if (rawJson != null && !rawJson.isBlank()) {
            e.setResponseJson(rawJson);
        }
        return pagoExternoRepository.save(e);
    }

    private static String safe(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

