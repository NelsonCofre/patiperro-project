package com.patiperro.pagos.service;

import com.patiperro.pagos.dto.comprobante.ComprobantePagoResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Renderer HTML simple (email-friendly) para Resumen de Transacción.
 * Importante: se escapa TODO input dinámico para evitar inyección/XSS.
 */
@Component
public class ComprobantePagoHtmlRenderer {

    private static final Locale LOCALE_CL = new Locale("es", "CL");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public String render(ComprobantePagoResponse c) {
        if (c == null) {
            return "";
        }

        String titulo = "Resumen de Transacción";
        String sub = safe(c.disclaimerLegal());

        StringBuilder sb = new StringBuilder(2048);
        sb.append("<!doctype html><html><head><meta charset=\"UTF-8\"></head><body ")
                .append("style=\"font-family:Arial,Helvetica,sans-serif;color:#111;margin:0;padding:16px;background:#f6f6f6;\">");

        sb.append("<div style=\"max-width:680px;margin:0 auto;background:#fff;border:1px solid #e5e5e5;border-radius:8px;overflow:hidden;\">");
        sb.append("<div style=\"padding:16px 18px;border-bottom:1px solid #eee;background:#fafafa;\">")
                .append("<div style=\"font-size:18px;font-weight:700;\">").append(escape(titulo)).append("</div>")
                .append("<div style=\"margin-top:6px;font-size:12px;color:#555;\">").append(escape(sub)).append("</div>")
                .append("</div>");

        sb.append("<div style=\"padding:18px;\">");

        // Tabla de datos (layout email-friendly)
        sb.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"border-collapse:collapse;\">");
        row(sb, "ID Reserva", String.valueOf(c.idReserva()));
        row(sb, "ID Orden (Patiperro)", String.valueOf(c.idOrden()));
        row(sb, "ID Transacción Externa", safe(c.idTransaccionExterna()));
        row(sb, "Fecha/Hora Operación", fmtFechaHora(c.fechaHoraOperacion()));
        row(sb, "Paseador", safe(c.paseadorNombre()));
        row(sb, "Mascota", safe(c.mascotaNombre()));
        row(sb, "Fecha Paseo", fmtFecha(c.fechaPaseo()));
        row(sb, "Inicio", fmtFechaHora(c.horaInicio()));
        row(sb, "Fin", fmtFechaHora(c.horaFinal()));
        row(sb, "Duración", c.duracionMinutos() != null ? (c.duracionMinutos() + " min") : "");
        sb.append("</table>");

        sb.append("<div style=\"height:14px;\"></div>");
        sb.append("<div style=\"font-size:14px;font-weight:700;margin-bottom:8px;\">Detalle de monto</div>");

        sb.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"border-collapse:collapse;\">");
        moneyRow(sb, "Total pagado", c.montoTotal(), c.moneda());
        moneyRow(sb, "Comisión Patiperro", c.comisionApp(), c.moneda());
        moneyRow(sb, "Monto neto", c.montoNeto(), c.moneda());
        sb.append("</table>");

        sb.append("<div style=\"height:14px;\"></div>");
        sb.append("<div style=\"padding:12px 12px;background:#fff7e6;border:1px solid #ffe1ad;border-radius:6px;font-size:12px;color:#5a3b00;\">")
                .append(escape(safe(c.estadoFondos())))
                .append("</div>");

        sb.append("<div style=\"margin-top:14px;font-size:11px;color:#666;line-height:1.4;\">")
                .append("Este documento es informativo. No constituye boleta o factura legal.")
                .append("</div>");

        sb.append("</div>"); // content
        sb.append("</div>"); // card
        sb.append("</body></html>");
        return sb.toString();
    }

    private static void row(StringBuilder sb, String label, String value) {
        String v = value == null ? "" : value;
        sb.append("<tr>")
                .append("<td style=\"padding:6px 8px;border-bottom:1px solid #f0f0f0;font-size:12px;color:#555;width:42%;\">")
                .append(escape(label))
                .append("</td>")
                .append("<td style=\"padding:6px 8px;border-bottom:1px solid #f0f0f0;font-size:12px;color:#111;\">")
                .append(escape(v))
                .append("</td>")
                .append("</tr>");
    }

    private static void moneyRow(StringBuilder sb, String label, BigDecimal amount, String moneda) {
        String v = fmtMonto(amount, moneda);
        sb.append("<tr>")
                .append("<td style=\"padding:6px 8px;border-bottom:1px solid #f0f0f0;font-size:12px;color:#555;\">")
                .append(escape(label))
                .append("</td>")
                .append("<td style=\"padding:6px 8px;border-bottom:1px solid #f0f0f0;font-size:12px;color:#111;text-align:right;\">")
                .append(escape(v))
                .append("</td>")
                .append("</tr>");
    }

    private static String fmtMonto(BigDecimal amount, String moneda) {
        if (amount == null) {
            return "";
        }
        String cur = (moneda == null || moneda.isBlank()) ? "CLP" : moneda.trim();
        // Para CLP se suele mostrar sin decimales.
        BigDecimal normalized = "CLP".equalsIgnoreCase(cur) ? amount.setScale(0, java.math.RoundingMode.HALF_UP) : amount;
        NumberFormat nf = NumberFormat.getNumberInstance(LOCALE_CL);
        nf.setGroupingUsed(true);
        nf.setMaximumFractionDigits("CLP".equalsIgnoreCase(cur) ? 0 : 2);
        nf.setMinimumFractionDigits("CLP".equalsIgnoreCase(cur) ? 0 : 0);
        return nf.format(normalized) + " " + cur.toUpperCase(Locale.ROOT);
    }

    private static String fmtFecha(LocalDate d) {
        if (d == null) {
            return "";
        }
        return FECHA.format(d);
    }

    private static String fmtFechaHora(LocalDateTime t) {
        if (t == null) {
            return "";
        }
        return FECHA_HORA.format(t);
    }

    private static String safe(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.isEmpty() ? "" : t;
    }

    /**
     * Escape HTML mínimo para texto: &lt; &gt; &amp; &quot; &#39;
     */
    private static String escape(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(Math.min(1024, s.length() + 16));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}

