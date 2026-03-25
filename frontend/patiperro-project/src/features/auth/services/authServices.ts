import { API_ENDPOINTS } from "../../../config/api";

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
    throw new Error(readApiErrorMessage(data, "No se pudo completar el registro del paseador."));
  }

  const body = data as AuthResponse & { message?: string };
  return {
    mensaje: body.mensaje ?? body.message,
    correo: body.correo
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

  return {
    mensaje: data?.mensaje ?? data?.message,
    correo: data?.correo
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

  let data: (AuthResponse & { message?: string }) | null = null;
  try {
    data = (await response.json()) as AuthResponse & { message?: string };
  } catch {
    data = null;
  }

  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "Correo o contraseña incorrectos."));
  }

  return {
    mensaje: data?.mensaje ?? data?.message,
    correo: data?.correo
  };
}
