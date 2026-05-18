package com.patiperro.notification_service.dto;

/** Clave pública VAPID para {@code pushManager.subscribe()} en el navegador. */
public record VapidPublicKeyResponse(String publicKey) {
}
