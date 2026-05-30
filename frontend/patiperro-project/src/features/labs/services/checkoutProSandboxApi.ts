import { API_ENDPOINTS } from "../../../config/api";

export type CrearPreferenciaRequest = {
  idReserva: number;
  montoTotal: number;
  tituloItem: string;
};

export type CrearPreferenciaResponse = {
  preferenceId: string;
  initPoint: string;
  sandboxInitPoint: string;
  urlCheckout: string;
};

const PAGOS_INTERNO_SECRET =
  typeof import.meta !== "undefined" ? (import.meta.env.VITE_PAGOS_INTERNO_SECRET ?? "").trim() : "";

function normalizarError(errorPayload: unknown, fallback: string): string {
  if (errorPayload && typeof errorPayload === "object" && "error" in errorPayload) {
    const err = (errorPayload as { error?: unknown }).error;
    if (typeof err === "string" && err.trim() !== "") {
      return err.trim();
    }
  }
  return fallback;
}

export async function crearPreferenciaCheckoutPro(
  body: CrearPreferenciaRequest
): Promise<CrearPreferenciaResponse> {
  if (!PAGOS_INTERNO_SECRET) {
    throw new Error("Falta VITE_PAGOS_INTERNO_SECRET para invocar el endpoint interno de pagos-service.");
  }

  const response = await fetch(API_ENDPOINTS.pagos.checkoutProPreferencias, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Patiperro-Interno-Secret": PAGOS_INTERNO_SECRET
    },
    body: JSON.stringify(body)
  });

  const json = (await response.json().catch(() => null)) as unknown;
  if (!response.ok) {
    throw new Error(normalizarError(json, `No se pudo crear la preferencia (HTTP ${response.status}).`));
  }
  return json as CrearPreferenciaResponse;
}
