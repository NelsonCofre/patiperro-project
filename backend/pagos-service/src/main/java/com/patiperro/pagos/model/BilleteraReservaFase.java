package com.patiperro.pagos.model;

/**
 * Estado del vínculo reserva ↔ montos del paseador en billetera (antes de liberación N+2).
 */
public enum BilleteraReservaFase {
    EN_RETENIDO,
    EN_VERIFICACION
}
