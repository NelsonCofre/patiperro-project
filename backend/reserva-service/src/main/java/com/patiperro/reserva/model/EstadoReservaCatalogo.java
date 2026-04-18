package com.patiperro.reserva.model;

/**
 * Catálogo {@code estado_reserva} esperado en BD (flujo paseador: solicitud → aceptar/rechazar → en curso → finalizada).
 * Asegurar filas con estos {@code id_estado_reserva} / {@code nombre_estado} en PostgreSQL.
 */
public final class EstadoReservaCatalogo {

    public static final int ID_SOLICITADA = 1;
    public static final int ID_ACEPTADA = 2;
    public static final int ID_RECHAZADA = 3;
    public static final int ID_EN_CURSO = 4;
    public static final int ID_FINALIZADA = 5;

    public static final String NOMBRE_SOLICITADA = "SOLICITADA";
    public static final String NOMBRE_ACEPTADA = "ACEPTADA";
    public static final String NOMBRE_RECHAZADA = "RECHAZADA";
    public static final String NOMBRE_EN_CURSO = "EN CURSO";
    public static final String NOMBRE_FINALIZADA = "FINALIZADA";

    private EstadoReservaCatalogo() {
    }
}
