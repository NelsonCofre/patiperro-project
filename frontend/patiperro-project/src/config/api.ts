// Base comun del backend/gateway consumido por el frontend.
export const API_BASE_URL = "http://localhost:8080";

/** Tras login paseador se guarda idPaseador para llamadas /api/agenda/bloques/usuario/{id}. */
export const PASEADOR_ID_SESSION_KEY = "patiperro_paseador_id";

/** Tras login/registro tutor se guarda id para GET /api/tutores/{id} (coordenadas de direccion). */
export const TUTOR_ID_SESSION_KEY = "patiperro_tutor_id";

// Endpoints centralizados para evitar URLs repetidas en paginas y servicios.
export const API_ENDPOINTS = {
  auth: {
    tutores: {
      register: `${API_BASE_URL}/api/auth/tutores/register`,
      login: `${API_BASE_URL}/api/auth/tutores/login`,
      uploadFotoPerfil: `${API_BASE_URL}/api/tutores/auth/upload-foto-perfil`
    },
    paseadores: {
      register: `${API_BASE_URL}/api/paseadores/auth/register`,
      login: `${API_BASE_URL}/api/paseadores/auth/login`,
      uploadFotoPerfil: `${API_BASE_URL}/api/paseadores/auth/upload-foto-perfil`,
      /** Catálogo público (sin JWT). */
      publicTamanos: `${API_BASE_URL}/api/paseadores/public/tamanos`,
      /** Configuración del paseador autenticado (JWT en cookie vía gateway). */
      meConfiguracion: `${API_BASE_URL}/api/paseadores/me/configuracion`,
      /**
       * Búsqueda pública por proximidad (Haversine + radio de cobertura). Query params obligatorios:
       * latitudReferencia, longitudReferencia; opcionales: radioBusquedaMaxKm, limite, y filtro agenda
       * (fechaDisponibilidad, horaInicioDisponibilidad, horaFinDisponibilidad, idEstadoBloqueDisponible).
       */
      publicCercanos: `${API_BASE_URL}/api/paseadores/public/cercanos`
    }
  },
  /** Mascotas (JWT tutor en cookie vía gateway). */
  mascotas: {
    base: `${API_BASE_URL}/api/mascotas`,
    especies: `${API_BASE_URL}/api/mascotas/especies`,
    razas: (especieId?: number) =>
      especieId != null
        ? `${API_BASE_URL}/api/mascotas/razas?especieId=${especieId}`
        : `${API_BASE_URL}/api/mascotas/razas`,
    tamanos: `${API_BASE_URL}/api/mascotas/tamanos`
  },
  /** Agenda (JWT en cookie vía gateway; mismo access_token que paseador/tutor). */
  agenda: {
    estadosBloque: `${API_BASE_URL}/api/agenda/estados-bloque`,
    diasSemana: `${API_BASE_URL}/api/agenda/dias-semana`,
    bloques: `${API_BASE_URL}/api/agenda/bloques`,
    bloquesSerieMes: `${API_BASE_URL}/api/agenda/bloques/serie-mes`,
    bloquesPorUsuario: (idUsuario: number) =>
      `${API_BASE_URL}/api/agenda/bloques/usuario/${idUsuario}`,
    bloquesOferta: (idUsuario: number, desde: string, hasta: string) =>
      `${API_BASE_URL}/api/agenda/bloques/usuario/${idUsuario}/oferta?desde=${desde}&hasta=${hasta}`,
    bloque: (idBloque: number) => `${API_BASE_URL}/api/agenda/bloques/${idBloque}`
  },
  /** Perfil tutor (JWT en cookie vía gateway). */
  tutores: {
    byId: (idTutor: number) => `${API_BASE_URL}/api/tutores/${idTutor}`
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
