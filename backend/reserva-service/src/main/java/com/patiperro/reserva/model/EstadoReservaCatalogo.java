package com.patiperro.reserva.model;

/**
 * Catálogo {@code estado_reserva} esperado en BD (flujo paseador: solicitud → aceptar/rechazar).
 */
public final class EstadoReservaCatalogo {

    public static final int ID_SOLICITADA = 1;
    public static final int ID_ACEPTADA = 2;
    public static final int ID_RECHAZADA = 3;

    public static final String NOMBRE_SOLICITADA = "SOLICITADA";
    public static final String NOMBRE_ACEPTADA = "ACEPTADA";
    public static final String NOMBRE_RECHAZADA = "RECHAZADA";

    private EstadoReservaCatalogo() {
    }
}
