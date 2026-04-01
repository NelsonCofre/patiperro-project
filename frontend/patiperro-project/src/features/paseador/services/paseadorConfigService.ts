// Cliente HTTP para configuración de servicio del paseador (tarifas, radio).
// GET catálogo público; GET/PUT /me/configuracion siempre con credentials (cookie JWT vía gateway).
import { API_ENDPOINTS } from "../../../config/api";

export type TamanoPublicDTO = {
  id: number;
  nombre: string;
  descripcion: string | null;
};

export type TarifaConfiguracionDTO = {
  tamanoId: number;
  tamanoNombre: string;
  precioPorHora: number;
};

export type ConfiguracionPaseadorDTO = {
  configuracionId: number | null;
  radioCoberturaKm: number | null;
  tarifas: TarifaConfiguracionDTO[];
};

export type UpsertConfiguracionBody = {
  radioCoberturaKm: number;
  tarifas: { tamanoId: number; precioPorHora: number }[];
};

function readErrorMessage(data: unknown, fallback: string): string {
  if (data && typeof data === "object") {
    const o = data as { message?: string; error?: string; mensaje?: string };
    const m = o.message ?? o.error ?? o.mensaje;
    if (typeof m === "string" && m.trim()) return m;
  }
  return fallback;
}

export async function fetchPublicTamanos(): Promise<TamanoPublicDTO[]> {
  const res = await fetch(API_ENDPOINTS.auth.paseadores.publicTamanos, {
    method: "GET",
    credentials: "include"
  });
  let data: unknown = null;
  try {
    data = await res.json();
  } catch {
    data = null;
  }
  if (!res.ok) {
    throw new Error(readErrorMessage(data, "No se pudo cargar los tamaños."));
  }
  if (!Array.isArray(data)) {
    throw new Error("Respuesta inválida del catálogo de tamaños.");
  }
  return data as TamanoPublicDTO[];
}

export async function getMyConfiguracion(): Promise<ConfiguracionPaseadorDTO> {
  const res = await fetch(API_ENDPOINTS.auth.paseadores.meConfiguracion, {
    method: "GET",
    credentials: "include"
  });
  let data: unknown = null;
  try {
    data = await res.json();
  } catch {
    data = null;
  }
  if (res.status === 401 || res.status === 403) {
    throw new Error(
      "Sesión requerida: inicia sesión como paseador (el JWT debe enviarse en la cookie hacia el gateway)."
    );
  }
  if (!res.ok) {
    throw new Error(readErrorMessage(data, "No se pudo cargar tu configuración."));
  }
  return data as ConfiguracionPaseadorDTO;
}

export async function putMyConfiguracion(
  body: UpsertConfiguracionBody
): Promise<ConfiguracionPaseadorDTO> {
  const res = await fetch(API_ENDPOINTS.auth.paseadores.meConfiguracion, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(body)
  });
  let data: unknown = null;
  try {
    data = await res.json();
  } catch {
    data = null;
  }
  if (res.status === 401 || res.status === 403) {
    throw new Error("Sesión expirada o sin permiso. Vuelve a iniciar sesión como paseador.");
  }
  if (!res.ok) {
    throw new Error(readErrorMessage(data, "No se pudo guardar la configuración."));
  }
  return data as ConfiguracionPaseadorDTO;
}
