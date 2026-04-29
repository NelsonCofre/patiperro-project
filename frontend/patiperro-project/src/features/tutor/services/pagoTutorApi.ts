import { API_ENDPOINTS } from "../../../config/api";
import { bearerAuthHeaders } from "../../../config/authHeaders";

type CheckoutSimuladoPayload = {
  idReserva: number;
  montoTotal: number;
  descripcion: string;
};

type CheckoutSimuladoResponse = {
  ok: boolean;
  provider: "MERCADO_PAGO_SANDBOX";
  status: "APPROVED" | "REJECTED" | "PENDING";
  paymentId: string;
};

async function parseJsonSafe(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

function buildFakeSuccess(idReserva: number): CheckoutSimuladoResponse {
  return {
    ok: true,
    provider: "MERCADO_PAGO_SANDBOX",
    status: "APPROVED",
    paymentId: `SIM-MP-${idReserva}-${Date.now()}`
  };
}

/**
 * MVP: intenta usar endpoint backend de checkout simulado.
 * Si aun no existe en backend, cae a simulacion local para continuar el flujo UI.
 */
export async function iniciarCheckoutMercadoPagoSimulado(
  payload: CheckoutSimuladoPayload
): Promise<CheckoutSimuladoResponse> {
  try {
    const response = await fetch(API_ENDPOINTS.pagos.checkoutSimulado, {
      method: "POST",
      credentials: "include",
      headers: { ...bearerAuthHeaders(), "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    const data = await parseJsonSafe(response);
    if (response.ok && data && typeof data === "object") {
      return data as CheckoutSimuladoResponse;
    }
  } catch {
    // Fallback controlado al mock local.
  }

  await new Promise((resolve) => setTimeout(resolve, 1200));
  return buildFakeSuccess(payload.idReserva);
}
