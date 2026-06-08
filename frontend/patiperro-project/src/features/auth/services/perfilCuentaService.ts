import { API_ENDPOINTS, resolveApiUrl } from "../../../config/api";
import { bearerAuthHeaders } from "../../../config/authHeaders";

type ApiErrorBody = { message?: string; mensaje?: string };

export type MiPerfilDTO = {
  id: number;
  nombreCompleto: string;
  correo: string;
  telefono?: string | null;
  fotoPerfil?: string | null;
  biografia?: string | null;
};

export type CambiarContrasenaPayload = {
  contrasenaActual: string;
  contrasenaNueva: string;
};

function readApiErrorMessage(data: unknown, fallback: string): string {
  if (data && typeof data === "object") {
    const o = data as ApiErrorBody;
    if (typeof o.message === "string" && o.message.trim()) return o.message;
    if (typeof o.mensaje === "string" && o.mensaje.trim()) return o.mensaje;
  }
  return fallback;
}

async function parseJsonSafe(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

function mapMiPerfil(row: unknown): MiPerfilDTO {
  const data = (row ?? {}) as Record<string, unknown>;
  return {
    id: Number(data.id ?? 0),
    nombreCompleto: String(data.nombreCompleto ?? "Usuario"),
    correo: String(data.correo ?? ""),
    telefono: data.telefono != null ? String(data.telefono) : null,
    fotoPerfil: data.fotoPerfil != null ? String(data.fotoPerfil) : null,
    biografia: data.biografia != null ? String(data.biografia) : null
  };
}

export function resolveFotoPerfilUrl(fotoPerfil?: string | null): string {
  const path = String(fotoPerfil ?? "").trim();
  return path ? resolveApiUrl(path) : "";
}

export async function fetchTutorMiPerfil(): Promise<MiPerfilDTO> {
  const response = await fetch(API_ENDPOINTS.tutores.mePerfil, {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudo cargar tu perfil."));
  }
  return mapMiPerfil(data);
}

export async function updateTutorFotoPerfil(file: File): Promise<MiPerfilDTO> {
  const body = new FormData();
  body.append("file", file);
  const response = await fetch(API_ENDPOINTS.tutores.meFotoPerfil, {
    method: "PATCH",
    credentials: "include",
    headers: { ...bearerAuthHeaders() },
    body
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudo actualizar tu foto de perfil."));
  }
  const row = (data ?? {}) as { perfil?: unknown; fotoPerfil?: string };
  if (row.perfil && typeof row.perfil === "object") {
    return mapMiPerfil(row.perfil);
  }
  return mapMiPerfil({ fotoPerfil: row.fotoPerfil });
}

export async function changeTutorPassword(payload: CambiarContrasenaPayload): Promise<void> {
  const response = await fetch(API_ENDPOINTS.tutores.meContrasena, {
    method: "PATCH",
    credentials: "include",
    headers: { ...bearerAuthHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudo cambiar la contraseña."));
  }
}

export async function fetchPaseadorMiPerfil(): Promise<MiPerfilDTO> {
  const response = await fetch(API_ENDPOINTS.auth.paseadores.mePerfil, {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudo cargar tu perfil."));
  }
  return mapMiPerfil(data);
}

export async function updatePaseadorFotoPerfil(file: File): Promise<MiPerfilDTO> {
  const body = new FormData();
  body.append("file", file);
  const response = await fetch(API_ENDPOINTS.auth.paseadores.meFotoPerfil, {
    method: "PATCH",
    credentials: "include",
    headers: { ...bearerAuthHeaders() },
    body
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudo actualizar tu foto de perfil."));
  }
  const row = (data ?? {}) as { perfil?: unknown; fotoPerfil?: string };
  if (row.perfil && typeof row.perfil === "object") {
    return mapMiPerfil(row.perfil);
  }
  return mapMiPerfil({ fotoPerfil: row.fotoPerfil });
}

export async function changePaseadorPassword(payload: CambiarContrasenaPayload): Promise<void> {
  const response = await fetch(API_ENDPOINTS.auth.paseadores.meContrasena, {
    method: "PATCH",
    credentials: "include",
    headers: { ...bearerAuthHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudo cambiar la contraseña."));
  }
}
