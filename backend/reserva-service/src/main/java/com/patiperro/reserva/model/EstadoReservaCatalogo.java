package com.patiperro.reserva.model;

import java.util.List;

/**
 * Catálogo {@code estado_reserva} alineado con PostgreSQL.
 *
 * <p><strong>Flujo tutor / pasarela:</strong> típicamente {@link #NOMBRE_SOLICITADA} → opcional
 * {@link #NOMBRE_PENDIENTE_PAGO} (checkout abierto) → {@link #NOMBRE_PAGADA} cuando Mercado Pago confirma el cobro.
 * El dinero retenido no liquida al paseador hasta que el servicio finalice; ante rechazo, expiración o cierta
 * cancelación por tutor puede ejecutarse devolución vía API de la pasarela (fuera de este catálogo).</p>
 *
 * <p><strong>Flujo paseador:</strong> desde {@link #NOMBRE_SOLICITADA}, {@link #NOMBRE_PENDIENTE_PAGO} o {@link #NOMBRE_PAGADA}
 * (aún sin aceptación), el paseador pasa a {@link #NOMBRE_ACEPTADA} o {@link #NOMBRE_RECHAZADA}. Luego
 * {@link #NOMBRE_EN_CURSO} → {@link #NOMBRE_FINALIZADA}.</p>
 *
 * <p><strong>Cierres sin servicio:</strong> {@link #NOMBRE_RECHAZADA} (paseador),
 * {@link #NOMBRE_EXPIRADA} (plazo de respuesta agotado por job), {@link #NOMBRE_CANCELADA} (tutor antes de aceptación).
 * {@code RECHAZADA} no equivale a {@code CANCELADA}: distinto actor y significado.</p>
 */
public final class EstadoReservaCatalogo {

    /** Solicitud creada; el bloque puede estar reservado según reglas de agenda. */
    public static final int ID_SOLICITADA = 1;
    public static final int ID_ACEPTADA = 2;
    /** Rechazo explícito del paseador (distinto de cancelación por tutor). */
    public static final int ID_RECHAZADA = 3;
    public static final int ID_EN_CURSO = 4;
    public static final int ID_FINALIZADA = 5;
    /**
     * El tutor anula antes de que el paseador acepte, estando la reserva en {@link #NOMBRE_SOLICITADA},
     * {@link #NOMBRE_PENDIENTE_PAGO} o {@link #NOMBRE_PAGADA}. No es {@link #NOMBRE_RECHAZADA}.
     */
    public static final int ID_CANCELADA = 6;
    /** Checkout / pasarela pendiente de confirmación (IPN o webhook). */
    public static final int ID_PENDIENTE_PAGO = 7;
    /** Cobro aprobado en Mercado Pago; la reserva puede seguir esperando decisión del paseador. */
    public static final int ID_PAGADA = 8;
    /**
     * Plazo de aceptación del paseador superado sin decisión (job). Libera agenda;
     * si había cobro ({@link #NOMBRE_PAGADA}), aplica lógica de devolución en otros componentes.
     */
    public static final int ID_EXPIRADA = 9;

    public static final String NOMBRE_SOLICITADA = "SOLICITADA";
    public static final String NOMBRE_ACEPTADA = "ACEPTADA";
    public static final String NOMBRE_RECHAZADA = "RECHAZADA";
    public static final String NOMBRE_EN_CURSO = "EN CURSO";
    public static final String NOMBRE_FINALIZADA = "FINALIZADA";
    /**
     * Cancelación por el tutor antes de aceptación del paseador ({@link #NOMBRE_SOLICITADA},
     * {@link #NOMBRE_PENDIENTE_PAGO} o {@link #NOMBRE_PAGADA}). No usar para rechazo del paseador.
     */
    public static final String NOMBRE_CANCELADA = "CANCELADA";
    public static final String NOMBRE_PENDIENTE_PAGO = "PENDIENTE_PAGO";
    public static final String NOMBRE_PAGADA = "PAGADA";
    public static final String NOMBRE_EXPIRADA = "EXPIRADA";

    /**
     * Estados en los que puede aplicarse devolución MP y la marca {@code mercadopago_reembolso_procesado_en}
     * (rechazo paseador, expiración por plazo, cancelación tutor con cobro previo).
     */
    public static final List<Integer> IDS_ESTADO_REEMBOLSO_MERCADOPAGO = List.of(
            ID_RECHAZADA,
            ID_EXPIRADA,
            ID_CANCELADA);

    /**
     * Estados desde los que un cobro Mercado Pago aprobado (webhook) puede pasar la reserva a {@link #NOMBRE_PAGADA}.
     * Excluye cierres ({@link #IDS_ESTADO_REEMBOLSO_MERCADOPAGO}) y estados de paseo en curso o finalizado.
     */
    public static final List<Integer> IDS_ESTADO_ORIGEN_MARCAR_PAGADA_MERCADOPAGO = List.of(
            ID_SOLICITADA,
            ID_PENDIENTE_PAGO,
            ID_ACEPTADA);

    private EstadoReservaCatalogo() {
    }

    /**
     * {@code true} si la reserva puede recibir confirmación de cobro MP y transicionar a {@link #NOMBRE_PAGADA}.
     */
    public static boolean estadoAdmiteMarcarPagadaPorCobroMercadoPago(Integer idEstadoReserva) {
        if (idEstadoReserva == null) {
            return false;
        }
        return IDS_ESTADO_ORIGEN_MARCAR_PAGADA_MERCADOPAGO.contains(idEstadoReserva);
    }

    /**
     * Estados en los que el tutor puede abrir o reintentar Checkout Pro y en los que se registra
     * un intento MP no aprobado sin cambiar el estado de la reserva.
     */
    public static boolean estadoAdmiteCheckoutOReintentoMercadoPago(Integer idEstadoReserva) {
        if (idEstadoReserva == null) {
            return false;
        }
        return idEstadoReserva.equals(ID_SOLICITADA) || idEstadoReserva.equals(ID_PENDIENTE_PAGO);
    }
}
