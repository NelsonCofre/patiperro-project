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
