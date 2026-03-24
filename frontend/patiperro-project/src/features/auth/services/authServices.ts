import { API_ENDPOINTS } from "../../../config/api";

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

export type AuthResponse = {
  mensaje?: string;
  correo?: string;
};

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
