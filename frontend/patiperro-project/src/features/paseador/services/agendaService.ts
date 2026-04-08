// Cliente HTTP para microservicio agenda (bloques, catálogos, bloqueos por día).
// Mismo patrón que paseadorConfigService: gateway 8080 + credentials (cookie JWT).
import { API_ENDPOINTS, PASEADOR_ID_SESSION_KEY } from "../../../config/api";

export type EstadoBloqueDTO = {
  idEstado: number;
  nombre: string;
};

export type DiaSemanaDTO = {
  idDia: number;
  nombre: string;
};

export type AgendaBloqueDTO = {
  idAgenda: number;
  idUsuario: number;
  horaInicio: string;
  horaFinal: string;
  fecha: string;
  estadoBloque: EstadoBloqueDTO;
  diaSemana: DiaSemanaDTO;
};

export type AgendaBloqueCuerpo = {
  idUsuario: number;
  horaInicio: string;
  horaFinal: string;
  fecha: string;
  estadoBloque: { idEstado: number };
  diaSemana: { idDia: number };
};

/** POST /api/agenda/bloques/serie-mes — mismo día de la semana en todo el mes de fechaSemilla. */
export type AgendaBloqueSerieMensualCuerpo = {
  idUsuario: number;
  fechaSemilla: string;
  horaInicio: string;
  horaFinal: string;
  estadoBloque: { idEstado: number };
};

export type AgendaBloqueSerieMensualRespuesta = {
  creados: number;
  omitidosPasado: number;
  omitidosSolape: number;
  bloques: AgendaBloqueDTO[];
};

export type AgendaBloqueoDiaDTO = {
  idBloqueo: number;
  idUsuario: number;
  fecha: string;
  motivo: string | null;
  creadoEn: string | null;
};

export type AgendaBloqueoDiaCuerpo = {
  idUsuario: number;
  fecha: string;
  motivo?: string | null;
};

function readErrorMessage(data: unknown, fallback: string): string {
  if (data && typeof data === "object") {
    const o = data as { message?: string; error?: string; mensaje?: string };
    const m = o.message ?? o.error ?? o.mensaje;
    if (typeof m === "string" && m.trim()) return m;
  }
  return fallback;
}

export function readStoredPaseadorId(): number | null {
  const raw = sessionStorage.getItem(PASEADOR_ID_SESSION_KEY);
  if (raw == null || raw === "") return null;
  const n = Number(raw);
  return Number.isFinite(n) ? n : null;
}

async function parseJson(res: Response): Promise<unknown> {
  try {
    return await res.json();
  } catch {
    return null;
  }
}

async function guardarSesionRequerida(res: Response, data: unknown): Promise<void> {
  if (res.status === 401 || res.status === 403) {
    throw new Error(
      readErrorMessage(
        data,
        "Sesión requerida: inicia sesión como paseador (JWT en cookie hacia el gateway)."
      )
    );
  }
}

export async function fetchEstadosBloque(): Promise<EstadoBloqueDTO[]> {
  const res = await fetch(API_ENDPOINTS.agenda.estadosBloque, {
    method: "GET",
    credentials: "include"
  });
  const data = await parseJson(res);
  await guardarSesionRequerida(res, data);
  if (!res.ok) {
    throw new Error(readErrorMessage(data, "No se pudieron cargar los estados de bloque."));
  }
  if (!Array.isArray(data)) {
    throw new Error("Respuesta inválida (estados de bloque).");
  }
  return data as EstadoBloqueDTO[];
}

export async function fetchDiasSemana(): Promise<DiaSemanaDTO[]> {
  const res = await fetch(API_ENDPOINTS.agenda.diasSemana, {
    method: "GET",
    credentials: "include"
  });
  const data = await parseJson(res);
  await guardarSesionRequerida(res, data);
  if (!res.ok) {
    throw new Error(readErrorMessage(data, "No se pudieron cargar los días de la semana."));
  }
  if (!Array.isArray(data)) {
    throw new Error("Respuesta inválida (días de la semana).");
  }
  return data as DiaSemanaDTO[];
}

export async function fetchBloquesPorUsuario(idUsuario: number): Promise<AgendaBloqueDTO[]> {
  const res = await fetch(API_ENDPOINTS.agenda.bloquesPorUsuario(idUsuario), {
    method: "GET",
    credentials: "include"
  });
  const data = await parseJson(res);
  await guardarSesionRequerida(res, data);
  if (!res.ok) {
    throw new Error(readErrorMessage(data, "No se pudieron cargar los bloques de agenda."));
  }
  if (!Array.isArray(data)) {
    throw new Error("Respuesta inválida (bloques).");
  }
  return data as AgendaBloqueDTO[];
}

export async function fetchBloquesOferta(
  idUsuario: number,
  desde: string,
  hasta: string
): Promise<AgendaBloqueDTO[]> {
  const res = await fetch(API_ENDPOINTS.agenda.bloquesOferta(idUsuario, desde, hasta), {
    method: "GET",
    credentials: "include"
  });
  const data = await parseJson(res);
  await guardarSesionRequerida(res, data);
  if (!res.ok) {
    throw new Error(readErrorMessage(data, "No se pudieron cargar los bloques ofertables."));
  }
  if (!Array.isArray(data)) {
    throw new Error("Respuesta inválida (bloques oferta).");
  }
  return data as AgendaBloqueDTO[];
}

export async function crearSerieMensualBloques(
  body: AgendaBloqueSerieMensualCuerpo
): Promise<AgendaBloqueSerieMensualRespuesta> {
  const res = await fetch(API_ENDPOINTS.agenda.bloquesSerieMes, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(body)
  });
  const data = await parseJson(res);
  await guardarSesionRequerida(res, data);
  if (!res.ok) {
    throw new Error(readErrorMessage(data, "No se pudo crear la serie de bloques."));
  }
  return data as AgendaBloqueSerieMensualRespuesta;
}

export async function crearBloque(body: AgendaBloqueCuerpo): Promise<AgendaBloqueDTO> {
  const res = await fetch(API_ENDPOINTS.agenda.bloques, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(body)
  });
  const data = await parseJson(res);
  await guardarSesionRequerida(res, data);
  if (!res.ok) {
    throw new Error(readErrorMessage(data, "No se pudo crear el bloque."));
  }
  return data as AgendaBloqueDTO;
}

export async function actualizarBloque(
  idBloque: number,
  body: AgendaBloqueCuerpo
): Promise<AgendaBloqueDTO> {
  const res = await fetch(API_ENDPOINTS.agenda.bloque(idBloque), {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(body)
  });
  const data = await parseJson(res);
  await guardarSesionRequerida(res, data);
  if (!res.ok) {
    throw new Error(readErrorMessage(data, "No se pudo actualizar el bloque."));
  }
  return data as AgendaBloqueDTO;
}

export async function eliminarBloque(idBloque: number): Promise<void> {
  const res = await fetch(API_ENDPOINTS.agenda.bloque(idBloque), {
    method: "DELETE",
    credentials: "include"
  });
  if (res.status === 204) return;
  const data = await parseJson(res);
  await guardarSesionRequerida(res, data);
  throw new Error(readErrorMessage(data, "No se pudo eliminar el bloque."));
}

export async function fetchBloqueosDiaPorUsuario(idUsuario: number): Promise<AgendaBloqueoDiaDTO[]> {
  const res = await fetch(API_ENDPOINTS.agenda.bloqueosDiaPorUsuario(idUsuario), {
    method: "GET",
    credentials: "include"
  });
  const data = await parseJson(res);
  await guardarSesionRequerida(res, data);
  if (!res.ok) {
    throw new Error(readErrorMessage(data, "No se pudieron cargar los bloqueos de día."));
  }
  if (!Array.isArray(data)) {
    throw new Error("Respuesta inválida (bloqueos de día).");
  }
  return data as AgendaBloqueoDiaDTO[];
}

export async function crearBloqueoDia(body: AgendaBloqueoDiaCuerpo): Promise<AgendaBloqueoDiaDTO> {
  const res = await fetch(API_ENDPOINTS.agenda.bloqueosDia, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(body)
  });
  const data = await parseJson(res);
  await guardarSesionRequerida(res, data);
  if (!res.ok) {
    throw new Error(readErrorMessage(data, "No se pudo crear el bloqueo de día."));
  }
  return data as AgendaBloqueoDiaDTO;
}

export async function eliminarBloqueoDia(id: number): Promise<void> {
  const res = await fetch(API_ENDPOINTS.agenda.bloqueoDia(id), {
    method: "DELETE",
    credentials: "include"
  });
  if (res.status === 204) return;
  const data = await parseJson(res);
  await guardarSesionRequerida(res, data);
  throw new Error(readErrorMessage(data, "No se pudo eliminar el bloqueo de día."));
}
