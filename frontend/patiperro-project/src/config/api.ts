export const API_BASE_URL = "http://localhost:8080";

export const API_ENDPOINTS = {
  auth: {
    tutores: {
      register: `${API_BASE_URL}/api/auth/tutores/register`,
      login: `${API_BASE_URL}/api/auth/tutores/login`,
      uploadFotoPerfil: `${API_BASE_URL}/api/auth/tutores/upload-foto-perfil`
    },
    paseadores: {
      register: `${API_BASE_URL}/api/paseadores/auth/register`,
      login: `${API_BASE_URL}/api/paseadores/auth/login`
    }
  }
};

/** Prefijo para resolver rutas relativas guardadas en el backend (ej. foto de perfil). */
export function resolveApiUrl(pathOrUrl: string): string {
  if (!pathOrUrl) return "";
  if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
    return pathOrUrl;
  }
  return `${API_BASE_URL.replace(/\/$/, "")}${pathOrUrl.startsWith("/") ? "" : "/"}${pathOrUrl}`;
}
