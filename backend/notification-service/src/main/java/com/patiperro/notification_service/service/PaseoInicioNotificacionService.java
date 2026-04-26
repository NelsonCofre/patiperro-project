package com.patiperro.notification_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio para eventos de inicio de paseo notificados vía integración interna
 * (p. ej. {@code reserva-service} → {@code POST /internal/paseo/inicio}).
 * Paso 9: placeholder; aquí se podrá enchufar Brevo, colas, plantillas, etc.
 */
@Service
public class PaseoInicioNotificacionService {

    private static final Logger log = LoggerFactory.getLogger(PaseoInicioNotificacionService.class);

    /**
     * Procesa el aviso de que un paseo ha comenzado (identificador de reserva en el dominio agregado).
     *
     * @param idReserva id de reserva, no nulo
     * @throws IllegalArgumentException si {@code idReserva} es nulo
     */
    public void procesarNotificacionInicioPaseo(Integer idReserva) {
        if (idReserva == null) {
            throw new IllegalArgumentException("idReserva es obligatorio");
        }
        log.info("Paseo inicio: procesamiento de notificaciones (placeholder), idReserva={}", idReserva);
    }
}
