import { API_ENDPOINTS, resolveApiUrl } from "../../../config/api";
import { bearerAuthHeaders } from "../../../config/authHeaders";
import { readStoredPaseadorId } from "./agendaService";
import type {
  DecisionSolicitudPayload,
  SolicitudPendientePaseador
} from "../types/solicitudPaseador.types";

type ApiErrorBody = { message?: string; mensaje?: string };

function readApiErrorMessage(data: unknown, fallback: string): string {
  if (data && typeof data === "object") {
    const o = data as ApiErrorBody;
    if (typeof o.message === "string" && o.message.trim()) return o.message;
    if (typeof o.mensaje === "string" && o.mensaje.trim()) return o.mensaje;
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

/** Respuesta de {@code GET /api/reserva/paseador/{id}/solicitudes-pendientes}. */
export type ReservaPaseadorSolicitudApiDTO = {
  idReserva: number;
  idTutorUsuario: number;
  idMascota: number;
  idAgendaBloque: number;
  montoTotal: number;
  fechaSolicitud: string | null;
  nombreEstado: string | null;
  fechaAgenda: string;
  horaInicio: string;
  horaFin: string;
  comuna: string;
  direccionReferencia: string;
  tutorNombre: string;
  tutorTelefono: string;
  tutorCorreo: string;
  tutorFotoUrl: string;
  tutorNotas: string;
  /** URL relativa o absoluta desde mascotas-service (integración interna reserva ↔ mascotas). */
  mascotaFotoUrl?: string | null;
  mascotaNombre: string;
  mascotaRaza?: string | null;
  mascotaTamano?: string | null;
  mascotaEdad?: string | null;
  mascotaPeso?: string | null;
  mascotaSexo?: string | null;
  mascotaCaracter?: string | null;
  mascotaCuidados?: string | null;
  codigoEncuentro?: number | null;
};

function mapNombreEstadoToUi(nombre: string | null | undefined): SolicitudPendientePaseador["estado"] {
  const u = (nombre ?? "").toUpperCase();
  if (u.includes("SOLICIT")) return "Solicitada";
  if (u.includes("ACEPT")) return "Aceptada";
  if (u.includes("RECHAZ")) return "Rechazada";
  return "Solicitada";
}

const PLACEHOLDER_MASCOTA_FOTO =
  "https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&w=400&q=60";

function mascotaFotoDesdeApi(s: ReservaPaseadorSolicitudApiDTO): string {
  const raw = s.mascotaFotoUrl?.trim();
  if (!raw) return PLACEHOLDER_MASCOTA_FOTO;
  return resolveApiUrl(raw);
}

function mapApiToSolicitud(s: ReservaPaseadorSolicitudApiDTO): SolicitudPendientePaseador {
  const dash = "—";
  const fotoTutor = s.tutorFotoUrl ? resolveApiUrl(s.tutorFotoUrl) : "";
  const fechaSol = s.fechaSolicitud?.trim() || "";

  return {
    idReserva: s.idReserva,
    tutorNombre: s.tutorNombre || `Tutor #${s.idTutorUsuario}`,
    tutorTelefono: s.tutorTelefono?.trim() || dash,
    tutorCorreo: s.tutorCorreo?.trim() || dash,
    tutorComuna: s.comuna?.trim() || dash,
    tutorDireccion: s.direccionReferencia?.trim() || dash,
    tutorFotoUrl: fotoTutor || "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=60",
    tutorNotas: s.tutorNotas?.trim() || undefined,
    mascotaNombre: s.mascotaNombre || `Mascota #${s.idMascota}`,
    mascotaFotoUrl: mascotaFotoDesdeApi(s),
    mascotaRaza: s.mascotaRaza?.trim() || dash,
    mascotaTamano: s.mascotaTamano?.trim() || dash,
    mascotaEdad: s.mascotaEdad?.trim() || dash,
    mascotaPeso: s.mascotaPeso?.trim() || dash,
    mascotaSexo: s.mascotaSexo?.trim() || dash,
    mascotaCaracter: s.mascotaCaracter?.trim() || dash,
    mascotaCuidados: s.mascotaCuidados?.trim() || dash,
    fecha: s.fechaAgenda?.trim() || "",
    horaInicio: s.horaInicio?.trim() || "",
    horaFin: s.horaFin?.trim() || "",
    comuna: s.comuna?.trim() || dash,
    direccionReferencia: s.direccionReferencia?.trim() || dash,
    montoTotal: Number(s.montoTotal) || 0,
    estado: mapNombreEstadoToUi(s.nombreEstado),
    codigoEncuentro: s.codigoEncuentro ?? null,
    comentarioTutor: undefined,
    fechaSolicitud: fechaSol
  };
}

export async function fetchSolicitudesPendientesPaseador(): Promise<SolicitudPendientePaseador[]> {
  const idPaseador = readStoredPaseadorId();
  if (idPaseador == null) {
    throw new Error("No se encontró id de paseador en sesión. Vuelve a iniciar sesión.");
  }

  const response = await fetch(API_ENDPOINTS.reserva.paseadorSolicitudesPendientes(idPaseador), {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudieron cargar las solicitudes."));
  }
  if (!Array.isArray(data)) {
    return [];
  }
  return (data as ReservaPaseadorSolicitudApiDTO[]).map(mapApiToSolicitud);
}

export async function responderSolicitudPaseador(
  idReserva: number,
  payload: DecisionSolicitudPayload
): Promise<{ idReserva: number; estado: "Aceptada" | "Rechazada" }> {
  if (!Number.isFinite(idReserva) || idReserva <= 0) {
    throw new Error("La solicitud seleccionada no es válida.");
  }

  const body: {
    decision: "ACEPTAR" | "RECHAZAR";
    motivoRechazo?: string;
    detalleRechazo?: string;
  } = {
    decision: payload.decision
  };
  if (payload.decision === "RECHAZAR") {
    if (payload.motivoRechazo && payload.motivoRechazo.trim()) {
      body.motivoRechazo = payload.motivoRechazo.trim();
    }
    if (payload.detalleRechazo && payload.detalleRechazo.trim()) {
      body.detalleRechazo = payload.detalleRechazo.trim();
    }
  }

  const response = await fetch(API_ENDPOINTS.reserva.status(idReserva), {
    method: "PATCH",
    credentials: "include",
    headers: { ...bearerAuthHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudo registrar la decisión."));
  }

  const row = data as { nombreEstado?: string | null };
  const nombre = row?.nombreEstado ?? "";
  const estado: "Aceptada" | "Rechazada" = nombre.toUpperCase().includes("RECHAZ")
    ? "Rechazada"
    : "Aceptada";

  return { idReserva, estado };
}

export async function validarCodigoEncuentroPaseador(
  idReserva: number,
  codigoIngresado: string
): Promise<{ idReserva: number; estado: string; fechaInicioReal?: string | null }> {
  const response = await fetch(API_ENDPOINTS.reservas.validarCodigo, {
    method: "POST",
    credentials: "include",
    headers: { ...bearerAuthHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify({
      idReserva,
      codigoIngresado
    })
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudo validar el código."));
  }
  const row = data as { idReserva: number; nombreEstado?: string; fechaInicioReal?: string | null };
  return {
    idReserva: row.idReserva,
    estado: row.nombreEstado ?? "EN CURSO",
    fechaInicioReal: row.fechaInicioReal ?? null
  };
}
