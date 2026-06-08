import { API_ENDPOINTS } from "../../../config/api";
import { bearerAuthHeaders } from "../../../config/authHeaders";

type ApiErrorBody = { message?: string; mensaje?: string };

function readError(data: unknown, fallback: string): string {
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

export type TutorDireccionApi = {
  comuna?: string | null;
  ciudad?: string | null;
  calle?: string | null;
  numeracion?: number | null;
  casaDepartamento?: string | null;
  latitud?: number | null;
  longitud?: number | null;
};

export type TutorPerfilResponse = {
  id?: number;
  direccion?: TutorDireccionApi | null;
};

export type PaseadorCercanoApi = {
  idPaseador: number;
  nombreCompleto: string;
  fotoPerfil?: string | null;
  biografia?: string | null;
  distanciaKm: number;
  radioCoberturaKm?: number | string | null;
  latitud: number;
  longitud: number;
  calificacionPromedio?: number;
  tarifaDesde?: number;
  /** Badge de identidad aprobada (API paseadores-service). */
  esVerificado?: boolean;
  /** @deprecated usar esVerificado; se acepta por compatibilidad temporal */
  verificado?: boolean;
};

export type PaseadorResumenPublicoApi = {
  idPaseador?: number;
  nombreCompleto?: string;
  fotoPerfil?: string | null;
};

export async function fetchPaseadorResumenPublico(idPaseador: number): Promise<PaseadorResumenPublicoApi> {
  const response = await fetch(API_ENDPOINTS.auth.paseadores.publicResumen(idPaseador), {
    method: "GET",
    credentials: "include"
  });

  let data: unknown = null;
  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (!response.ok) {
    throw new Error(readError(data, "No se pudo cargar el resumen del paseador."));
  }

  return (data ?? {}) as PaseadorResumenPublicoApi;
}

export async function fetchTutorPorId(idTutor: number): Promise<TutorPerfilResponse> {
  const response = await fetch(API_ENDPOINTS.tutores.byId(idTutor), {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });

  let data: unknown = null;
  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (!response.ok) {
    throw new Error(readError(data, "No se pudo cargar el perfil del tutor."));
  }

  return data as TutorPerfilResponse;
}

export async function fetchPaseadoresCercanos(params: {
  latitudReferencia: number;
  longitudReferencia: number;
  radioBusquedaMaxKm: number;
  limite?: number;
  fechaDisponibilidad?: string;
  horaInicioDisponibilidad?: string;
  horaFinDisponibilidad?: string;
  idEstadoBloqueDisponible?: number;
  soloVerificados?: boolean;
}): Promise<PaseadorCercanoApi[]> {
  const limite = params.limite ?? 50;
  const qs = new URLSearchParams({
    latitudReferencia: String(params.latitudReferencia),
    longitudReferencia: String(params.longitudReferencia),
    radioBusquedaMaxKm: String(params.radioBusquedaMaxKm),
    limite: String(limite)
  });

  if (params.fechaDisponibilidad) {
    qs.set("fechaDisponibilidad", params.fechaDisponibilidad);
  }
  if (params.horaInicioDisponibilidad) {
    qs.set("horaInicioDisponibilidad", params.horaInicioDisponibilidad);
  }
  if (params.horaFinDisponibilidad) {
    qs.set("horaFinDisponibilidad", params.horaFinDisponibilidad);
  }
  if (params.idEstadoBloqueDisponible != null) {
    qs.set("idEstadoBloqueDisponible", String(params.idEstadoBloqueDisponible));
  }
  if (params.soloVerificados) {
    qs.set("soloVerificados", "true");
  }

  const response = await fetch(`${API_ENDPOINTS.auth.paseadores.publicCercanos}?${qs.toString()}`, {
    method: "GET"
  });

  let data: unknown = null;
  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (!response.ok) {
    throw new Error(readError(data, "No se pudieron cargar los paseadores cercanos."));
  }

  if (!Array.isArray(data)) {
    return [];
  }

  return data as PaseadorCercanoApi[];
}
