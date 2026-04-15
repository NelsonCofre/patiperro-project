import { ACCESS_TOKEN_SESSION_KEY } from "./api";

/** Cabecera Authorization para el gateway cuando la cookie HttpOnly no llega (Vite ↔ :8080). */
export function bearerAuthHeaders(): Record<string, string> {
  const raw = sessionStorage.getItem(ACCESS_TOKEN_SESSION_KEY);
  if (!raw?.trim()) return {};
  return { Authorization: `Bearer ${raw.trim()}` };
}
