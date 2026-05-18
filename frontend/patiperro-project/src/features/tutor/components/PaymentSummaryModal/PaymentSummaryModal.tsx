import { createPortal } from "react-dom";
import { useEffect, useMemo, useRef, useState } from "react";
import { obtenerComprobantePagoTutor, type ComprobantePagoTutorResponse } from "../../services/pagoTutorApi";
import type { ReservaTutorDetalleDTO } from "../../types/reservaTutor.types";
import { formatReservaMoney } from "../../utils/reservaEstadoUtils";
import styles from "./PaymentSummaryModal.module.css";

type Props = {
  reserva: ReservaTutorDetalleDTO;
  onClose: () => void;
};

function formatDateTime(value?: string | null | unknown): string {
  if (value == null || value === "") return "Fecha no disponible";
  if (typeof value === "object") return "Fecha no disponible";
  const s = String(value).trim();
  if (!s) return "Fecha no disponible";
  const date = new Date(s);
  if (Number.isNaN(date.getTime())) {
    return s.length > 80 ? "Fecha no disponible" : s;
  }
  return new Intl.DateTimeFormat("es-CL", {
    dateStyle: "full",
    timeStyle: "short"
  }).format(date);
}

function getTransactionId(reserva: ReservaTutorDetalleDTO): string {
  return (
    reserva.paymentTransactionId?.trim() ||
    reserva.paymentOrderId?.trim() ||
    (reserva.idPago != null ? `TRX-${reserva.idPago}` : `TRX-${reserva.idReserva}`)
  );
}

function escapeHtml(value: string): string {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function buildFallbackFromReserva(reserva: ReservaTutorDetalleDTO): ComprobantePagoTutorResponse {
  return {
    tipoDocumento: "RESUMEN_TRANSACCION",
    disclaimerLegal: "Resumen de Transaccion (informativo). No constituye boleta o factura legal.",
    idReserva: reserva.idReserva,
    idOrden: reserva.idPago ?? reserva.idReserva,
    idTransaccionExterna: getTransactionId(reserva),
    fechaHoraOperacion: reserva.paymentConfirmedAt ?? reserva.fechaAceptacion ?? reserva.fechaSolicitud ?? null,
    paseadorNombre: reserva.paseadorNombre,
    mascotaNombre: reserva.mascotaNombre,
    fechaPaseo: reserva.fecha ?? null,
    horaInicio: reserva.horaInicio ?? null,
    horaFinal: reserva.horaFinal ?? null,
    duracionMinutos: null,
    moneda: "CLP",
    montoTotal: Number(reserva.montoTotal ?? 0),
    comisionApp: 0,
    montoNeto: Number(reserva.montoTotal ?? 0),
    estadoFondos:
      "Estado: Pago confirmado. Fondos retenidos en garantia por Patiperro hasta la finalizacion del servicio"
  };
}

function formatServiceDetail(data: ComprobantePagoTutorResponse): string {
  const fecha =
    data.fechaPaseo != null && String(data.fechaPaseo).trim() !== ""
      ? String(data.fechaPaseo)
      : "Fecha no disponible";
  const inicio = data.horaInicio ? formatDateTime(data.horaInicio) : "--:--";
  const fin = data.horaFinal ? formatDateTime(data.horaFinal) : "--:--";
  return `${fecha} · ${inicio} - ${fin}`;
}

function downloadSummaryAsHtml(data: ComprobantePagoTutorResponse): void {
  const transactionId = data.idTransaccionExterna || `TRX-${data.idOrden || data.idReserva}`;
  const operationDate = formatDateTime(data.fechaHoraOperacion);
  const total = formatReservaMoney(data.montoTotal);
  const html = `<!doctype html>
<html lang="es">
  <head>
    <meta charset="utf-8" />
    <title>Resumen de Transaccion ${escapeHtml(transactionId)}</title>
    <style>
      body { font-family: Arial, sans-serif; padding: 32px; color: #1f2937; }
      .wrap { max-width: 760px; margin: 0 auto; }
      .badge { display:inline-block; padding:8px 12px; border:1px solid #dce7dd; border-radius:8px; color:#2f7d35; font-weight:700; }
      h1 { color:#163020; margin:16px 0 8px; }
      .grid { display:grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap:16px; margin:24px 0; }
      .card { border:1px solid #dce7dd; border-radius:12px; padding:16px; background:#f8fbf8; }
      .label { font-size:12px; text-transform:uppercase; color:#64748b; font-weight:700; margin-bottom:8px; display:block; }
      .value { font-size:16px; color:#183120; font-weight:700; }
      .note { margin-top:24px; padding:16px; border:1px solid #bfdbfe; border-radius:12px; background:#eff6ff; line-height:1.7; }
      .legal { margin-top:16px; padding:16px; border:1px solid #fde68a; border-radius:12px; background:#fffbeb; line-height:1.7; }
    </style>
  </head>
  <body>
    <div class="wrap">
      <span class="badge">Resumen de Transaccion</span>
      <h1>Pago confirmado</h1>
      <p>Comprobante informativo de la operacion realizada en Patiperro.</p>
      <div class="grid">
        <div class="card"><span class="label">ID de transaccion</span><div class="value">${escapeHtml(transactionId)}</div></div>
        <div class="card"><span class="label">Fecha y hora</span><div class="value">${escapeHtml(operationDate)}</div></div>
        <div class="card"><span class="label">Paseador</span><div class="value">${escapeHtml(data.paseadorNombre)}</div></div>
        <div class="card"><span class="label">Mascota</span><div class="value">${escapeHtml(data.mascotaNombre)}</div></div>
        <div class="card"><span class="label">Detalle del servicio</span><div class="value">${escapeHtml(formatServiceDetail(data))}</div></div>
        <div class="card"><span class="label">Monto total pagado</span><div class="value">${escapeHtml(total)}</div></div>
      </div>
      <div class="note">
        Estado: Pago confirmado. Fondos retenidos en garantia por Patiperro hasta la finalizacion del servicio.
      </div>
      <div class="legal">
        Este documento corresponde a un Resumen de Transaccion y no constituye una boleta o factura legal.
      </div>
    </div>
  </body>
</html>`;

  const blob = new Blob([html], { type: "text/html;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `resumen-transaccion-${transactionId}.html`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export default function PaymentSummaryModal({ reserva, onClose }: Props) {
  const [data, setData] = useState<ComprobantePagoTutorResponse>(() => buildFallbackFromReserva(reserva));
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const reservaRef = useRef(reserva);
  reservaRef.current = reserva;

  useEffect(() => {
    let cancelled = false;
    const idReserva = reserva.idReserva;

    async function load() {
      setLoading(true);
      setError("");
      try {
        const result = await obtenerComprobantePagoTutor(idReserva);
        if (cancelled) return;
        setData(result);
      } catch (e) {
        if (cancelled) return;
        setError(e instanceof Error ? e.message : "No se pudo cargar el comprobante real.");
        setData(buildFallbackFromReserva(reservaRef.current));
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }
    void load();
    return () => {
      cancelled = true;
    };
  }, [reserva.idReserva]);

  const transactionId = useMemo(
    () => data.idTransaccionExterna || `TRX-${data.idOrden || data.idReserva}`,
    [data.idOrden, data.idReserva, data.idTransaccionExterna]
  );

  const { operationDate, serviceDetail } = useMemo(() => {
    try {
      return {
        operationDate: formatDateTime(data.fechaHoraOperacion),
        serviceDetail: formatServiceDetail(data)
      };
    } catch {
      return {
        operationDate: "Fecha no disponible",
        serviceDetail: "Detalle no disponible"
      };
    }
  }, [data]);

  return createPortal(
    <div className={styles.overlay}>
      <section className={styles.modal} role="dialog" aria-modal="true">
        <div className={styles.header}>
          <div>
            <p className={styles.eyebrow}>Resumen de Transaccion</p>
            <h2>Comprobante de pago</h2>
            <p className={styles.description}>
              Este documento es un respaldo informativo del pago confirmado en la plataforma.
            </p>
          </div>
          <button type="button" className={styles.closeButton} onClick={onClose}>
            Cerrar
          </button>
        </div>

        <div className={styles.grid}>
          <article className={styles.card}>
            <span>ID de transaccion</span>
            <strong>{transactionId}</strong>
          </article>
          <article className={styles.card}>
            <span>Fecha y hora</span>
            <strong>{operationDate}</strong>
          </article>
          <article className={styles.card}>
            <span>Paseador</span>
            <strong>{data.paseadorNombre}</strong>
          </article>
          <article className={styles.card}>
            <span>Mascota</span>
            <strong>{data.mascotaNombre}</strong>
          </article>
          <article className={styles.card}>
            <span>Detalle del servicio</span>
            <strong>{serviceDetail}</strong>
          </article>
          <article className={styles.card}>
            <span>Monto total pagado</span>
            <strong>{formatReservaMoney(data.montoTotal)}</strong>
          </article>
        </div>

        <div className={styles.noteBox}>
          {typeof data.estadoFondos === "string" && data.estadoFondos.trim()
            ? data.estadoFondos
            : "Estado: Pago confirmado. Fondos retenidos en garantia por Patiperro hasta la finalizacion del servicio."}
        </div>

        <div className={styles.emailBox}>
          Se considera una copia de respaldo para el correo registrado del tutor una vez confirmado el pago por backend.
        </div>

        <div className={styles.legalBox}>
          {typeof data.disclaimerLegal === "string" && data.disclaimerLegal.trim() ? (
            data.disclaimerLegal
          ) : (
            <>
              Este documento corresponde a un <strong>Resumen de Transaccion</strong> y no constituye una boleta o
              factura legal.
            </>
          )}
        </div>

        {error ? <div className={styles.emailBox}>Aviso: {error}</div> : null}

        {loading ? (
          <div className={styles.loadingOverlay} role="status" aria-live="polite">
            Cargando comprobante real...
          </div>
        ) : null}

        <div className={styles.footer}>
          <button
            type="button"
            className={styles.downloadButton}
            onClick={() => downloadSummaryAsHtml(data)}
          >
            Descargar resumen
          </button>
        </div>
      </section>
    </div>,
    document.body
  );
}
