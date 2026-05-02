import { API_ENDPOINTS } from "../../../config/api";
import { bearerAuthHeaders } from "../../../config/authHeaders";

export type CheckoutProResponse = {
  initPoint: string;
  preferenceId: string | null;
};

async function parseJsonSafe(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

/**
 * Crea preferencia Checkout Pro en pagos-service y devuelve la URL de redirección a Mercado Pago.
 */
export async function iniciarCheckoutPro(idReserva: number): Promise<CheckoutProResponse> {
  const response = await fetch(API_ENDPOINTS.pagos.checkoutPro, {
    method: "POST",
    credentials: "include",
    headers: { ...bearerAuthHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify({ idReserva })
  });
  const data = await parseJsonSafe(response);
  if (!response.ok || !data || typeof data !== "object") {
    const msg =
      response.status === 403
        ? "No tienes permiso para pagar esta reserva."
        : response.status === 404
          ? "Reserva no encontrada o no disponible para pago."
          : response.status === 409
            ? "La reserva no está en un estado que permita iniciar el pago."
            : response.status === 502
              ? "La pasarela de pago no respondió. Revisa el token de Mercado Pago en pagos-service."
              : "No se pudo iniciar el pago. Intenta de nuevo.";
    throw new Error(msg);
  }
  const initPoint = (data as { initPoint?: string }).initPoint;
  if (!initPoint || typeof initPoint !== "string") {
    throw new Error("Respuesta de checkout inválida (sin initPoint).");
  }
  const preferenceId = (data as { preferenceId?: string | null }).preferenceId ?? null;
  return { initPoint, preferenceId };
}
