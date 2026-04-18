// Servicios HTTP del modulo auth.
// Concentran la comunicacion del frontend con login, registro y subida de fotos.
import {
  ACCESS_TOKEN_SESSION_KEY,
  API_ENDPOINTS,
  PASEADOR_ID_SESSION_KEY,
  TUTOR_ID_SESSION_KEY
} from "../../../config/api";

type ApiErrorBody = { message?: string; mensaje?: string };

function readApiErrorMessage(data: unknown, fallback: string): string {
  if (data && typeof data === "object") {
    const o = data as ApiErrorBody;
    if (typeof o.message === "string" && o.message.trim()) {
      return o.message;
    }
    if (typeof o.mensaje === "string" && o.mensaje.trim()) {
      return o.mensaje;
    }
  }
  return fallback;
}

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
  /** Login/registro paseador: id numérico para /api/agenda/bloques/usuario/{id}. */
  idPaseador?: number;
  /** Login/registro tutor: id para GET /api/tutores/{id}. */
  idTutor?: number;
  /** Mismo JWT que la cookie; para Authorization: Bearer en desarrollo (Vite). */
  accessToken?: string;
};

function persistAccessToken(body: { accessToken?: string } | null | undefined) {
  const t = body?.accessToken;
  if (typeof t === "string" && t.trim()) {
    sessionStorage.setItem(ACCESS_TOKEN_SESSION_KEY, t.trim());
  }
}

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
    throw new Error(readApiErrorMessage(data, "No se pudo subir la foto de perfil."));
  }

  return data.url;
}

/**
 * Subida de foto de perfil del paseador (multipart); devuelve ruta relativa al gateway.
 */
export async function uploadPaseadorProfilePhoto(file: File): Promise<string> {
  const body = new FormData();
  body.append("file", file);

  const response = await fetch(API_ENDPOINTS.auth.paseadores.uploadFotoPerfil, {
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
    throw new Error(readApiErrorMessage(data, "No se pudo subir la foto de perfil."));
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
    throw new Error(readApiErrorMessage(data, "No se pudo completar el registro del tutor."));
  }

  const body = (data as AuthResponse & { message?: string; idTutor?: number }) ?? {};
  const idTutor =
    typeof body.idTutor === "number" && Number.isFinite(body.idTutor) ? body.idTutor : undefined;
  if (idTutor != null) {
    sessionStorage.setItem(TUTOR_ID_SESSION_KEY, String(idTutor));
  }
  persistAccessToken(body);

  return {
    mensaje: body.mensaje ?? body.message,
    correo: body.correo,
    idTutor,
    accessToken: body.accessToken
  };
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
    throw new Error(readApiErrorMessage(data, "No se pudo completar el registro del paseador."));
  }

  const body = data as AuthResponse & { message?: string; idPaseador?: number };
  const idPaseador =
    typeof body.idPaseador === "number" && Number.isFinite(body.idPaseador) ? body.idPaseador : undefined;
  persistAccessToken(body);
  return {
    mensaje: body.mensaje ?? body.message,
    correo: body.correo,
    idPaseador,
    accessToken: body.accessToken
  };
}

export async function loginTutor(credentials: {
  correo: string;
  contrasena: string;
}): Promise<AuthResponse> {
  const response = await fetch(API_ENDPOINTS.auth.tutores.login, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    credentials: "include",
    body: JSON.stringify({
      correo: credentials.correo,
      contrasena: credentials.contrasena
    })
  });

  let data: (AuthResponse & { message?: string }) | null = null;
  try {
    data = (await response.json()) as AuthResponse & { message?: string };
  } catch {
    data = null;
  }

  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "Correo o contraseña incorrectos."));
  }

  const body = data as AuthResponse & { message?: string; idTutor?: number };
  const idTutor =
    typeof body.idTutor === "number" && Number.isFinite(body.idTutor) ? body.idTutor : undefined;
  if (idTutor != null) {
    sessionStorage.setItem(TUTOR_ID_SESSION_KEY, String(idTutor));
  }
  persistAccessToken(body);

  return {
    mensaje: body.mensaje ?? body.message,
    correo: body.correo,
    idTutor,
    accessToken: body.accessToken
  };
}

export async function loginPaseador(credentials: {
  correo: string;
  contrasena: string;
}): Promise<AuthResponse> {
  const response = await fetch(API_ENDPOINTS.auth.paseadores.login, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    credentials: "include",
    body: JSON.stringify({
      correo: credentials.correo,
      contrasena: credentials.contrasena
    })
  });

  let data: (AuthResponse & { message?: string; idPaseador?: number }) | null = null;
  try {
    data = (await response.json()) as AuthResponse & { message?: string; idPaseador?: number };
  } catch {
    data = null;
  }

  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "Correo o contraseña incorrectos."));
  }

  const idPaseador =
    data && typeof data.idPaseador === "number" && Number.isFinite(data.idPaseador)
      ? data.idPaseador
      : undefined;
  if (idPaseador != null) {
    sessionStorage.setItem(PASEADOR_ID_SESSION_KEY, String(idPaseador));
  }
  persistAccessToken(data ?? undefined);

  return {
    mensaje: data?.mensaje ?? data?.message,
    correo: data?.correo,
    idPaseador,
    accessToken: data?.accessToken
  };
}
