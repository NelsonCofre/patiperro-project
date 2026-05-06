/**
 * Mercado Pago — SDK frontend.
 * Prioriza VITE_MERCADOPAGO_PUBLIC_KEY; fallback al valor entregado para pruebas locales.
 */
export const MERCADOPAGO_PUBLIC_KEY =
  (typeof import.meta !== "undefined" ? import.meta.env.VITE_MERCADOPAGO_PUBLIC_KEY : "") ||
  "APP_USR-76e22f21-12a1-418c-b02c-c03c8f25d7d9";

export function getMercadoPagoPublicKey(): string {
  return MERCADOPAGO_PUBLIC_KEY.trim();
}
