// Base comun del backend/gateway consumido por el frontend.
export const API_BASE_URL = "http://localhost:8080";

// Endpoints centralizados para evitar URLs repetidas en paginas y servicios.
export const API_ENDPOINTS = {
  auth: {
    tutores: {
      register: `${API_BASE_URL}/api/auth/tutores/register`,
      login: `${API_BASE_URL}/api/auth/tutores/login`,
      uploadFotoPerfil: `${API_BASE_URL}/api/auth/tutores/upload-foto-perfil`
    },
    paseadores: {
      register: `${API_BASE_URL}/api/paseadores/auth/register`,
      login: `${API_BASE_URL}/api/paseadores/auth/login`,
      uploadFotoPerfil: `${API_BASE_URL}/api/paseadores/auth/upload-foto-perfil`
    }
  }
};

/** Prefijo para resolver rutas relativas guardadas en el backend (ej. foto de perfil). */
export function resolveApiUrl(pathOrUrl: string): string {
  // Evita errores si el backend devuelve una ruta vacia.
  if (!pathOrUrl) return "";
  // Si ya es una URL completa, no necesita transformacion.
  if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
    return pathOrUrl;
  }
  // Completa rutas relativas usando la base del gateway.
  return `${API_BASE_URL.replace(/\/$/, "")}${pathOrUrl.startsWith("/") ? "" : "/"}${pathOrUrl}`;
}
