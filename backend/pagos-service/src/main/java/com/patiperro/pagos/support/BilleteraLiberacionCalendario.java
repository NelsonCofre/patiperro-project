package com.patiperro.pagos.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Regla calendario N+2 para liberación (misma que {@code BilleteraLiberacionTransaccionalService} y respuesta API).
 */
public final class BilleteraLiberacionCalendario {

    private BilleteraLiberacionCalendario() {}

    /** Día calendario N (cierre del servicio) según {@code fecha_fin_servicio} en {@code zone}. */
    public static LocalDate diaOrigenServicio(LocalDateTime fechaFinServicio, ZoneId zone) {
        if (fechaFinServicio == null || zone == null) {
            return null;
        }
        return fechaFinServicio.atZone(zone).toLocalDate();
    }

    /**
     * Primer instante del día calendario N+2 en {@code zone} (inicio del día en que el batch puede pasar fondos a
     * disponible, salvo disputa).
     */
    public static Instant inicioDiaDisponibleParaRetiro(LocalDateTime fechaFinServicio, ZoneId zone) {
        LocalDate diaFin = diaOrigenServicio(fechaFinServicio, zone);
        if (diaFin == null) {
            return null;
        }
        return diaFin.plusDays(2).atStartOfDay(zone).toInstant();
    }

    /** {@code true} si {@code hoy} (en {@code zone}) ya alcanzó el día N+2 respecto al cierre del servicio. */
    public static boolean diaActualPermiteLiberacion(LocalDate hoy, LocalDateTime fechaFinServicio, ZoneId zone) {
        LocalDate diaFin = diaOrigenServicio(fechaFinServicio, zone);
        if (diaFin == null || hoy == null || zone == null) {
            return false;
        }
        LocalDate disponibleDesde = diaFin.plusDays(2);
        return !hoy.isBefore(disponibleDesde);
    }
}
