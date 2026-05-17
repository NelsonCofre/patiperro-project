package com.patiperro.notification_service.dto;

/**
 * Resultado de {@code PagoNotificacionService#procesarLiberacionFondosConsolidadaPaseador} para no sobrecargar un único {@code boolean}.
 */
public record LiberacionFondosNotificacionResult(
        /** Si es {@code true}, el caller debe responder 204; si {@code false}, 502 (fallo downstream). */
        boolean respuestaHttpExitosa,
        /** {@code true} cuando no había correo: no se llamó a Brevo (no es lo mismo que “correo enviado”). */
        boolean omitidoSinCorreo,
        /** {@code true} si se intentó POST a Brevo (éxito o excepción). */
        boolean intentoEnvioBrevo
) {
    /** Sin destinatario: HTTP 204 pero no hubo envío Brevo. */
    public static LiberacionFondosNotificacionResult cuandoSinCorreo() {
        return new LiberacionFondosNotificacionResult(true, true, false);
    }

    /** Brevo respondió OK para el envío transactional. */
    public static LiberacionFondosNotificacionResult cuandoBrevoAcepta() {
        return new LiberacionFondosNotificacionResult(true, false, true);
    }

    /** Excepción o fallo al llamar a Brevo → HTTP 502. */
    public static LiberacionFondosNotificacionResult cuandoBrevoFalla() {
        return new LiberacionFondosNotificacionResult(false, false, true);
    }

    /** Entrada inválida en capa servicio (no debe ocurrir si el controller filtra bien). */
    public static LiberacionFondosNotificacionResult cuandoArgumentosInvalidos() {
        return new LiberacionFondosNotificacionResult(false, false, false);
    }
}
