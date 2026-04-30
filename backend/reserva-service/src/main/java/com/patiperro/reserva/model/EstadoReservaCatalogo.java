package com.patiperro.reserva.model;

/**
 * Catálogo {@code estado_reserva} alineado con PostgreSQL.
 * <p>Flujo paseador: solicitada → aceptada o {@linkplain #NOMBRE_RECHAZADA rechazada} (decisión del paseador)
 * → en curso → finalizada.</p>
 * <p>{@linkplain #NOMBRE_CANCELADA Cancelada}: el tutor retira la solicitud mientras sigue solicitada;
 * no reutiliza {@code RECHAZADA}, que reserva el significado “rechazo por el paseador”.</p>
 */
public final class EstadoReservaCatalogo {

    public static final int ID_SOLICITADA = 1;
    public static final int ID_ACEPTADA = 2;
    /** Rechazo explícito del paseador (distinto de cancelación por tutor). */
    public static final int ID_RECHAZADA = 3;
    public static final int ID_EN_CURSO = 4;
    public static final int ID_FINALIZADA = 5;
    /** Anulación por el tutor con solicitud aún pendiente; no es {@link #NOMBRE_RECHAZADA} (rechazo del paseador). */
    public static final int ID_CANCELADA = 6;
    /** El tutor debe pagar la reserva; a la espera de confirmación de pasarela. */
    public static final int ID_PENDIENTE_PAGO = 7;
    /** Pago confirmado por pasarela (webhook/IPN). */
    public static final int ID_PAGADA = 8;

    public static final String NOMBRE_SOLICITADA = "SOLICITADA";
    public static final String NOMBRE_ACEPTADA = "ACEPTADA";
    public static final String NOMBRE_RECHAZADA = "RECHAZADA";
    public static final String NOMBRE_EN_CURSO = "EN CURSO";
    public static final String NOMBRE_FINALIZADA = "FINALIZADA";
    /** Retiro del tutor; no usar {@link #NOMBRE_RECHAZADA} para este caso. */
    public static final String NOMBRE_CANCELADA = "CANCELADA";
    public static final String NOMBRE_PENDIENTE_PAGO = "PENDIENTE_PAGO";
    public static final String NOMBRE_PAGADA = "PAGADA";

    private EstadoReservaCatalogo() {
    }
}
