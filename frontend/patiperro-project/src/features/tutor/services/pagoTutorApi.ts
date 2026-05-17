import { API_ENDPOINTS } from "../../../config/api";
import { bearerAuthHeaders } from "../../../config/authHeaders";

export type PagoBrickResponse = {
  mpPaymentId: string;
  mpStatus: string;
  mpStatusDetail: string;
};

export type CheckoutProPreferenciaResponse = {
  preferenceId: string;
  initPoint: string;
  sandboxInitPoint: string;
  urlCheckout: string;
};

export type ComprobantePagoTutorResponse = {
  tipoDocumento: string;
  disclaimerLegal: string;
  idReserva: number;
  idOrden: number;
  idTransaccionExterna: string;
  fechaHoraOperacion: string | null;
  paseadorNombre: string;
  mascotaNombre: string;
  fechaPaseo: string | null;
  horaInicio: string | null;
  horaFinal: string | null;
  duracionMinutos: number | null;
  moneda: string;
  montoTotal: number;
  comisionApp: number;
  montoNeto: number;
  estadoFondos: string;
};

export type BrickPagoRequestBody = {
  idReserva: number;
  token: string;
  paymentMethodId: string;
  installments: number;
  issuerId?: string;
  payerEmail: string;
  identificationType?: string;
  identificationNumber?: string;
};

async function parseJsonSafe(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

/** BigDecimal u otros formatos del gateway → número seguro para la UI. */
function toNumberLoose(v: unknown): number {
  if (typeof v === "number" && Number.isFinite(v)) {
    return v;
  }
  if (typeof v === "string") {
    const n = Number(v.trim());
    return Number.isFinite(n) ? n : NaN;
  }
  return NaN;
}

/** Evita que JSON raro (objeto anidado) rompa React al renderizar hijos de texto. */
function coerceText(v: unknown): string {
  if (v == null) return "";
  if (typeof v === "string") return v;
  if (typeof v === "number" && Number.isFinite(v)) return String(v);
  if (typeof v === "boolean") return String(v);
  return "";
}

/**
 * Crea el pago en pagos-service con el token del Payment Brick (POST /v1/payments vía MP, sin preferencia).
 */
export async function procesarPagoBrick(body: BrickPagoRequestBody): Promise<PagoBrickResponse> {
  const response = await fetch(API_ENDPOINTS.pagos.pagoBrick, {
    method: "POST",
    credentials: "include",
    headers: { ...bearerAuthHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    const o = data && typeof data === "object" ? (data as { message?: string; detail?: string; title?: string }) : {};
    const fromServer = o.detail || o.message || o.title;
    if (fromServer) {
      throw new Error(fromServer);
    }
    const fallback =
      response.status === 400
        ? "Datos de pago inválidos o rechazados por Mercado Pago."
        : response.status === 403
          ? "No tienes permiso para pagar esta reserva."
          : response.status === 404
            ? "Reserva no encontrada."
            : response.status === 409
              ? "La reserva no está en un estado que permita el pago."
              : response.status === 502
                ? "La pasarela de pago no respondió. Revisa el token de Mercado Pago en pagos-service."
                : "No se pudo completar el pago. Intenta de nuevo.";
    throw new Error(fallback);
  }
  if (!data || typeof data !== "object") {
    throw new Error("Respuesta de pago inválida.");
  }
  const o = data as {
    mpPaymentId?: string;
    mpStatus?: string;
    mpStatusDetail?: string;
  };
  return {
    mpPaymentId: o.mpPaymentId ?? "",
    mpStatus: o.mpStatus ?? "",
    mpStatusDetail: o.mpStatusDetail ?? ""
  };
}

/**
 * Sincroniza un pago de Checkout Pro con Patiperro (misma lógica que el webhook de Mercado Pago).
 */
export async function sincronizarCheckoutProPago(paymentId: string): Promise<void> {
  const response = await fetch(API_ENDPOINTS.pagos.checkoutProSincronizarPagoTutor, {
    method: "POST",
    credentials: "include",
    headers: { ...bearerAuthHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify({ paymentId: paymentId.trim() })
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    const o = data && typeof data === "object" ? (data as { message?: string; detail?: string; title?: string }) : {};
    const fromServer = o.detail || o.message || o.title;
    if (fromServer) {
      throw new Error(fromServer);
    }
    throw new Error(`No se pudo sincronizar el pago (HTTP ${response.status}).`);
  }
}

export async function crearPreferenciaCheckoutProTutor(body: {
  idReserva: number;
  montoTotal: number;
  tituloItem: string;
}): Promise<CheckoutProPreferenciaResponse> {
  const response = await fetch(API_ENDPOINTS.pagos.checkoutProPreferenciaTutor, {
    method: "POST",
    credentials: "include",
    headers: { ...bearerAuthHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    const o = data && typeof data === "object" ? (data as { message?: string; detail?: string; title?: string }) : {};
    const fromServer = o.detail || o.message || o.title;
    if (fromServer) {
      throw new Error(fromServer);
    }
    throw new Error(`No se pudo crear la preferencia Checkout Pro (HTTP ${response.status}).`);
  }
  if (!data || typeof data !== "object") {
    throw new Error("Respuesta inválida al crear preferencia.");
  }
  const o = data as CheckoutProPreferenciaResponse;
  return {
    preferenceId: o.preferenceId ?? "",
    initPoint: o.initPoint ?? "",
    sandboxInitPoint: o.sandboxInitPoint ?? "",
    urlCheckout: o.urlCheckout ?? ""
  };
}

export async function obtenerComprobantePagoTutor(
  idReserva: number
): Promise<ComprobantePagoTutorResponse> {
  const response = await fetch(API_ENDPOINTS.pagos.comprobanteByReserva(idReserva), {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    const o = data && typeof data === "object" ? (data as { message?: string; detail?: string; title?: string }) : {};
    const fromServer = o.detail || o.message || o.title;
    if (fromServer) {
      throw new Error(fromServer);
    }
    throw new Error(`No se pudo obtener el comprobante (HTTP ${response.status}).`);
  }
  if (!data || typeof data !== "object") {
    throw new Error("Respuesta inválida del comprobante.");
  }
  const o = data as Record<string, unknown>;
  const str = (k: string): string | null => {
    const v = o[k];
    if (v == null) return null;
    if (typeof v === "string") return v;
    if (typeof v === "number" && Number.isFinite(v)) return String(v);
    if (Array.isArray(v) && v.length >= 3 && v.every((x) => typeof x === "number")) {
      const [y, mo, day, h = 0, m = 0, s = 0] = v as number[];
      const d = new Date(y, mo - 1, day, h, m, s);
      return Number.isNaN(d.getTime()) ? null : d.toISOString();
    }
    return null;
  };
  const fechaPaseoRaw = o.fechaPaseo;
  let fechaPaseo: string | null = typeof fechaPaseoRaw === "string" ? fechaPaseoRaw : null;
  if (
    fechaPaseo == null &&
    Array.isArray(fechaPaseoRaw) &&
    fechaPaseoRaw.length >= 3 &&
    fechaPaseoRaw.every((x) => typeof x === "number")
  ) {
    const [y, mo, d] = fechaPaseoRaw as number[];
    fechaPaseo = `${y}-${String(mo).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
  }
  const horaInicio = str("horaInicio");
  const horaFinal = str("horaFinal");
  const durRaw = o.duracionMinutos;
  const duracionMinutos =
    typeof durRaw === "number" && Number.isFinite(durRaw)
      ? durRaw
      : typeof durRaw === "string" && /^\d+$/.test(durRaw.trim())
        ? Number(durRaw.trim())
        : null;

  return {
    tipoDocumento: coerceText(o.tipoDocumento) || "RESUMEN_TRANSACCION",
    disclaimerLegal: coerceText(o.disclaimerLegal),
    idReserva: Number(o.idReserva ?? idReserva),
    idOrden: Number(o.idOrden ?? 0),
    idTransaccionExterna: coerceText(o.idTransaccionExterna),
    fechaHoraOperacion: str("fechaHoraOperacion"),
    paseadorNombre: coerceText(o.paseadorNombre),
    mascotaNombre: coerceText(o.mascotaNombre),
    fechaPaseo,
    horaInicio,
    horaFinal,
    duracionMinutos,
    moneda: coerceText(o.moneda) || "CLP",
    montoTotal: toNumberLoose(o.montoTotal),
    comisionApp: toNumberLoose(o.comisionApp),
    montoNeto: toNumberLoose(o.montoNeto),
    estadoFondos: coerceText(o.estadoFondos)
  };
}
