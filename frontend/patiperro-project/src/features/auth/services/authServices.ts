import { API_ENDPOINTS } from "../../../config/api";

type ApiErrorBody = { message?: string; mensaje?: string };

export type RegisterTutorPayload = {
  rut: string;
  primerNombre: string;
  segundoNombre: string;
  apellidoPaterno: string;
  apellidoMaterno: string;
  fechaNacimiento: string;
  telefono: string;
  correo: string;
  contrasena: string;
  fotoPerfil: string;
  biografia: string;
  pais: string;
  region: string;
  ciudad: string;
  calle: string;
  comuna: string;
  numeracion: number;
  casaDepartamento: string;
  fotos: string[];
};

export type RegisterPaseadorPayload = {
  rut: string;
  primerNombre: string;
  segundoNombre: string;
  apellidoPaterno: string;
  apellidoMaterno: string;
  fechaNacimiento: string;
  telefono: number;
  correo: string;
  contrasena: string;
  fotoPerfil: string;
  biografia: string;
  pais: string;
  region: string;
  ciudad: string;
  calle: string;
  comuna: string;
  numeracion: number;
  casaDepartamento: string;
  fotos: string[];
};

export type AuthResponse = {
  mensaje?: string;
  correo?: string;
};

/**
 * Sube la imagen de perfil y devuelve la ruta a guardar en registro (relativa al gateway).
 */
export async function uploadTutorProfilePhoto(file: File): Promise<string> {
  const body = new FormData();
  body.append("file", file);

  const response = await fetch(API_ENDPOINTS.auth.tutores.uploadFotoPerfil, {
    method: "POST",
    credentials: "include",
    body
  });

  let data: { url?: string } & ApiErrorBody | null = null;
  try {
    data = (await response.json()) as { url?: string } & ApiErrorBody;
  } catch {
    data = null;
  }

  if (!response.ok || !data?.url) {
    const msg =
      (data && "message" in data && data.message) ||
      (data && "mensaje" in data && data.mensaje) ||
      "No se pudo subir la foto de perfil.";
    throw new Error(msg);
  }

  return data.url;
}

export async function registerTutor(
  payload: RegisterTutorPayload
): Promise<AuthResponse> {
  const response = await fetch(API_ENDPOINTS.auth.tutores.register, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    credentials: "include",
    body: JSON.stringify(payload)
  });

  let data: AuthResponse | { message?: string } | null = null;

  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (!response.ok) {
    const errorMessage =
      (data && "message" in data && data.message) ||
      (data && "mensaje" in data && data.mensaje) ||
      "No se pudo completar el registro del tutor.";

    throw new Error(errorMessage);
  }

  return (data as AuthResponse) ?? {};
}

export async function registerPaseador(
  payload: RegisterPaseadorPayload
): Promise<AuthResponse> {
  const response = await fetch(API_ENDPOINTS.auth.paseadores.register, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    credentials: "include",
    body: JSON.stringify(payload)
  });

  let data: AuthResponse | { message?: string } | null = null;

  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (!response.ok) {
    const errorMessage =
      (data && "message" in data && data.message) ||
      (data && "mensaje" in data && data.mensaje) ||
      "No se pudo completar el registro del paseador.";

    throw new Error(errorMessage);
  }

  return (data as AuthResponse) ?? {};
}
