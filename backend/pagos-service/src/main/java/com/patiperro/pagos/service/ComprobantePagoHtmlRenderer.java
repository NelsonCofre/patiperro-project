package com.patiperro.pagos.service;

import com.patiperro.pagos.dto.comprobante.ComprobantePagoResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Genera HTML para descarga y correo del resumen de transacción.
 * <p>Todo texto procedente de dominio o externos se escapa con {@link HtmlUtils#htmlEscape}; no insertar HTML crudo.</p>
 */
@Component
public class ComprobantePagoHtmlRenderer {

    private static final Locale ES_CL = Locale.forLanguageTag("es-CL");

    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", ES_CL);

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", ES_CL);

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", ES_CL);

    private static final String EM_DASH = "\u2014";

    public String render(ComprobantePagoResponse r) {
        if (r == null) {
            throw new IllegalArgumentException("comprobante es obligatorio");
        }

        String dhOp =
                r.fechaHoraOperacion() != null ? FECHA_HORA.format(r.fechaHoraOperacion()) : EM_DASH;
        String fechaPaseo = r.fechaPaseo() != null ? FECHA.format(r.fechaPaseo()) : EM_DASH;
        String ini = r.horaInicio() != null ? HORA.format(r.horaInicio()) : EM_DASH;
        String fin = r.horaFinal() != null ? HORA.format(r.horaFinal()) : EM_DASH;
        String dur =
                r.duracionMinutos() != null ? r.duracionMinutos() + " minutos" : EM_DASH;

        String tipoDoc = r.tipoDocumento() != null ? r.tipoDocumento() : "Resumen de transacción";
        String disclaimer =
                r.disclaimerLegal() != null
                        ? r.disclaimerLegal()
                        : "Resumen de Transacción (informativo). No constituye boleta o factura legal.";

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1"/>
                  <title>Resumen de transacción — Patiperro</title>
                  <style>
                    body { font-family: system-ui, Segoe UI, Roboto, Helvetica, Arial, sans-serif; margin: 24px; color: #1a1a1a; }
                    h1 { font-size: 1.25rem; margin-bottom: 4px; }
                    .muted { color: #555; font-size: 0.9rem; margin-bottom: 20px; }
                    table { border-collapse: collapse; width: 100%%; max-width: 560px; margin-top: 12px; }
                    th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #e5e5e5; vertical-align: top; }
                    th { width: 42%%; color: #444; font-weight: 600; }
                    .legal { margin-top: 24px; padding: 12px; background: #f7f7f7; border-radius: 8px; font-size: 0.85rem; color: #444; }
                    .fondos { margin-top: 16px; padding: 12px; border-left: 4px solid #2563eb; background: #eff6ff; font-size: 0.9rem; }
                  </style>
                </head>
                <body>
                  <h1>"""
                + esc(tipoDoc)
                + """
                </h1>
                  <p class="muted">"""
                + esc(disclaimer)
                + """
                </p>
                  <table>
                    <tr><th>Reserva</th><td>#"""
                + esc(r.idReserva() != null ? String.valueOf(r.idReserva()) : EM_DASH)
                + """
                </td></tr>
                    <tr><th>Orden interna</th><td>"""
                + esc(r.idOrden() != null ? String.valueOf(r.idOrden()) : EM_DASH)
                + """
                </td></tr>
                    <tr><th>ID transacción pasarela</th><td>"""
                + esc(r.idTransaccionExterna() != null ? r.idTransaccionExterna() : EM_DASH)
                + """
                </td></tr>
                    <tr><th>Fecha y hora operación</th><td>"""
                + esc(dhOp)
                + """
                </td></tr>
                    <tr><th>Paseador</th><td>"""
                + esc(r.paseadorNombre() != null ? r.paseadorNombre() : EM_DASH)
                + """
                </td></tr>
                    <tr><th>Mascota</th><td>"""
                + esc(r.mascotaNombre() != null ? r.mascotaNombre() : EM_DASH)
                + """
                </td></tr>
                    <tr><th>Fecha del paseo</th><td>"""
                + esc(fechaPaseo)
                + """
                </td></tr>
                    <tr><th>Horario del servicio</th><td>"""
                + esc(ini + " – " + fin + " (" + dur + ")")
                + """
                </td></tr>
                    <tr><th>Moneda</th><td>"""
                + esc(r.moneda() != null ? r.moneda() : "CLP")
                + """
                </td></tr>
                    <tr><th>Monto total</th><td>"""
                + esc(formatClp(r.montoTotal()))
                + """
                </td></tr>
                    <tr><th>Comisión plataforma</th><td>"""
                + esc(formatClp(r.comisionApp()))
                + """
                </td></tr>
                    <tr><th>Monto neto servicio</th><td>"""
                + esc(formatClp(r.montoNeto()))
                + """
                </td></tr>
                  </table>
                  <div class="fondos">"""
                + esc(r.estadoFondos() != null ? r.estadoFondos() : EM_DASH)
                + """
                </div>
                  <p class="legal">Documento informativo generado por Patiperro. Para consultas sobre tu reserva utiliza la app.</p>
                </body>
                </html>
                """;
    }

    /** Escape HTML; {@code null} → cadena vacía (evita NPE y literales &quot;null&quot; en UI). */
    private static String esc(String s) {
        return HtmlUtils.htmlEscape(s != null ? s : "", "UTF-8");
    }

    /**
     * Montos en CLP para lectura humana; si el valor no cabe en {@code long}, se usa representación decimal sin notación científica.
     */
    private static String formatClp(BigDecimal v) {
        if (v == null) {
            return EM_DASH;
        }
        BigDecimal rounded = v.setScale(0, RoundingMode.HALF_UP);
        if (rounded.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0
                || rounded.compareTo(BigDecimal.valueOf(Long.MIN_VALUE)) < 0) {
            return rounded.toPlainString() + " CLP";
        }
        long n = rounded.longValue();
        return String.format(ES_CL, "%,d CLP", n);
    }
}
