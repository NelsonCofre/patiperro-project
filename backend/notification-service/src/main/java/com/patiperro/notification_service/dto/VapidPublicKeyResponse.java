package com.patiperro.notification_service.dto;

/**
 * Clave pública VAPID para {@code pushManager.subscribe()} en el navegador.
 * Es información pública por diseño (estándar Web Push).
 */
public record VapidPublicKeyResponse(String publicKey) {
}
