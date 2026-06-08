import { API_ENDPOINTS } from "../../../config/api";
import { bearerAuthHeaders } from "../../../config/authHeaders";
import type { DailyScheduleItem } from "../types/dailySchedule.types";
import { readStoredPaseadorId } from "./agendaService";

type DailyScheduleApiDTO = {
  idReserva?: number;
  idAgendaBloque?: number;
  mascotaNombre?: string | null;
  fechaAgenda?: string | null;
  horaInicio?: string | null;
  horaFin?: string | null;
  inicioProgramado?: string | null;
  finProgramado?: string | null;
  comuna?: string | null;
  direccionReferencia?: string | null;
  idEstadoReserva?: number | null;
  nombreEstado?: string | null;
};

function readApiErrorMessage(data: unknown, fallback: string): string {
  if (data && typeof data === "object") {
    const o = data as Record<string, unknown>;
    for (const key of ["message", "mensaje", "detail", "title", "error"]) {
      const value = o[key];
      if (typeof value === "string" && value.trim()) {
        return value.trim();
      }
    }
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

function normalizeText(value: string | null | undefined, fallback = ""): string {
  return typeof value === "string" ? value.trim() : fallback;
}

function buildFallbackIso(fechaAgenda: string, hora: string): string {
  if (!fechaAgenda || !hora) return "";
  return `${fechaAgenda}T${hora}`;
}

function toDailyScheduleItem(row: DailyScheduleApiDTO): DailyScheduleItem | null {
  if (!Number.isFinite(row.idReserva) || !Number.isFinite(row.idAgendaBloque)) {
    return null;
  }

  const fechaAgenda = normalizeText(row.fechaAgenda);
  const horaInicio = normalizeText(row.horaInicio);
  const horaFin = normalizeText(row.horaFin);
  const inicioProgramado = normalizeText(row.inicioProgramado) || buildFallbackIso(fechaAgenda, horaInicio);
  const finProgramado = normalizeText(row.finProgramado) || buildFallbackIso(fechaAgenda, horaFin);

  return {
    idReserva: Number(row.idReserva),
    idAgendaBloque: Number(row.idAgendaBloque),
    mascotaNombre: normalizeText(row.mascotaNombre, "Mascota sin nombre"),
    fechaAgenda,
    horaInicio,
    horaFin,
    inicioProgramado,
    finProgramado,
    comuna: normalizeText(row.comuna, "Sector pendiente"),
    direccionReferencia: normalizeText(row.direccionReferencia),
    idEstadoReserva:
      row.idEstadoReserva == null || !Number.isFinite(row.idEstadoReserva)
        ? null
        : Number(row.idEstadoReserva),
    nombreEstado: normalizeText(row.nombreEstado) || null
  };
}

function startTimeMs(item: DailyScheduleItem): number {
  const value = Date.parse(item.inicioProgramado);
  return Number.isFinite(value) ? value : Number.MAX_SAFE_INTEGER;
}

export async function fetchDailySchedulePanel(): Promise<DailyScheduleItem[]> {
  const idPaseador = readStoredPaseadorId();
  if (idPaseador == null) {
    throw new Error("No se encontró el id del paseador en sesión. Vuelve a iniciar sesión.");
  }

  const response = await fetch(API_ENDPOINTS.reserva.paseadorAgendaHoyPanel(idPaseador), {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudieron cargar los paseos de hoy."));
  }
  if (!Array.isArray(data)) {
    return [];
  }

  return (data as DailyScheduleApiDTO[])
    .map(toDailyScheduleItem)
    .filter((item): item is DailyScheduleItem => item != null)
    .sort((a, b) => startTimeMs(a) - startTimeMs(b));
}
