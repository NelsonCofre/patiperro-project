package com.patiperro.pagos.checkout;

/**
 * Valor de {@code external_reference} en Checkout Pro / pagos MP para enlazar el cobro con {@code reserva.id_reserva}.
 * <p>El webhook usa el sufijo tras el último {@code ':'} si el entero directo no parsea.</p>
 */
public final class MercadoPagoReservaExternalReference {

    public static final String PREFIX = "patiperro-reserva:";

    private MercadoPagoReservaExternalReference() {
    }

    public static String fromReservaId(long idReserva) {
        return PREFIX + idReserva;
    }

    public static String fromReservaId(Integer idReserva) {
        if (idReserva == null) {
            return null;
        }
        return PREFIX + idReserva;
    }
}
