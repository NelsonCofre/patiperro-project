import { API_ENDPOINTS, resolveApiUrl } from "../../../config/api";
import { bearerAuthHeaders } from "../../../config/authHeaders";
import { readStoredPaseadorId } from "./agendaService";
import type {
  DecisionSolicitudPayload,
  SolicitudPendientePaseador
} from "../types/solicitudPaseador.types";

type EstadoEncuentroApiDTO = {
  idReserva?: number;
  estadoEncuentro?: "PENDIENTE" | "CONFIRMADO" | "FALLIDO" | string;
  idEstadoReserva?: number | null;
  nombreEstadoReserva?: string | null;
  intentosFallidos?: number | null;
  bloqueadoHasta?: string | null;
  puedeReintentarEnSegundos?: number | null;
  mensaje?: string | null;
};

function readApiErrorMessage(data: unknown, fallback: string): string {
  if (data && typeof data === "object") {
    const o = data as Record<string, unknown>;
    for (const key of ["message", "mensaje", "detail", "title"]) {
      const v = o[key];
      if (typeof v === "string" && v.trim()) {
        return v.trim();
      }
    }
    const errors = o.errors;
    if (Array.isArray(errors) && errors.length > 0) {
      const first = errors[0] as Record<string, unknown>;
      const d = first.defaultMessage ?? first.message;
      if (typeof d === "string" && d.trim()) {
        return d.trim();
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
  fechaInicioReal?: string | null;
};

function mapNombreEstadoToUi(nombre: string | null | undefined): SolicitudPendientePaseador["estado"] {
  const u = (nombre ?? "").toUpperCase();
  if (u.includes("SOLICIT")) return "Solicitada";
  if (u.includes("PAGAD")) return "Pagada";
  if (u.includes("ACEPT")) return "Aceptada";
  if (u.includes("CURSO")) return "En Curso";
  if (u.includes("FINAL")) return "Finalizada";
  if (u.includes("DISPON") || u.includes("LIBER")) return "Disponible";
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
  const estado = mapNombreEstadoToUi(s.nombreEstado);
  const paseoIniciado = estado === "En Curso" || estado === "Finalizada";

  return {
    idReserva: s.idReserva,
    idTutorUsuario: s.idTutorUsuario,
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
    estado,
    codigoEncuentro: s.codigoEncuentro ?? null,
    comentarioTutor: undefined,
    fechaSolicitud: fechaSol,
    fechaInicioReal: s.fechaInicioReal ?? null,
    trackingActivo: paseoIniciado,
    chatActivo: paseoIniciado
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

/** Marca la reserva como FINALIZADA (solo desde EN_CURSO). Dispara liberación a verificación en billetera. */
export async function finalizarPaseoPaseador(idReserva: number): Promise<{ idReserva: number; estado: string }> {
  if (!Number.isFinite(idReserva) || idReserva <= 0) {
    throw new Error("La reserva no es válida.");
  }

  const response = await fetch(API_ENDPOINTS.reserva.status(idReserva), {
    method: "PATCH",
    credentials: "include",
    headers: { ...bearerAuthHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify({ decision: "FINALIZAR_PASEO" })
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudo finalizar el paseo."));
  }

  const row = data as { nombreEstado?: string | null };
  const nombre = (row?.nombreEstado ?? "").trim();
  return { idReserva, estado: nombre || "FINALIZADA" };
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
    throw new Error(readApiErrorMessage(data, `No se pudo validar el codigo (HTTP ${response.status}).`));
  }
  const row = data as { idReserva: number; nombreEstado?: string; fechaInicioReal?: string | null };
  return {
    idReserva: row.idReserva,
    estado: row.nombreEstado ?? "EN CURSO",
    fechaInicioReal: row.fechaInicioReal ?? null
  };
}

export type EstadoEncuentroReservaDTO = {
  idReserva: number;
  estadoEncuentro: "PENDIENTE" | "CONFIRMADO" | "FALLIDO";
  nombreEstadoReserva: string;
  intentosFallidos: number;
  bloqueadoHasta: string | null;
  puedeReintentarEnSegundos: number | null;
  mensaje: string | null;
};

export async function obtenerEstadoEncuentroReserva(
  idReserva: number
): Promise<EstadoEncuentroReservaDTO> {
  const response = await fetch(API_ENDPOINTS.reservas.estadoEncuentro(idReserva), {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudo consultar el estado del encuentro."));
  }
  const row = (data ?? {}) as EstadoEncuentroApiDTO;
  const estadoRaw = String(row.estadoEncuentro ?? "PENDIENTE").toUpperCase();
  const estado =
    estadoRaw === "CONFIRMADO" || estadoRaw === "FALLIDO" ? estadoRaw : "PENDIENTE";
  return {
    idReserva: row.idReserva ?? idReserva,
    estadoEncuentro: estado,
    nombreEstadoReserva: row.nombreEstadoReserva?.trim() || "",
    intentosFallidos: Number(row.intentosFallidos ?? 0),
    bloqueadoHasta: row.bloqueadoHasta ?? null,
    puedeReintentarEnSegundos:
      row.puedeReintentarEnSegundos == null ? null : Number(row.puedeReintentarEnSegundos),
    mensaje: row.mensaje?.trim() || null
  };
}
