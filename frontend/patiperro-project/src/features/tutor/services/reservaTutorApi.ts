import { API_ENDPOINTS, TUTOR_ID_SESSION_KEY } from "../../../config/api";
import { bearerAuthHeaders } from "../../../config/authHeaders";
import type { ReservaTutorDetalleDTO } from "../types/reservaTutor.types";

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

export type MascotaTutorDTO = {
  idMascota: number;
  nombre: string;
  tamanoId: number | null;
  tamanoNombre: string;
};

export type PaseadorPerfilDTO = {
  idUsuario: number;
  nombre: string;
  correo: string; // ¡El campo estrella que necesitamos!
  telefono?: string;
  // ... puedes agregar otros campos que devuelva tu backend si los necesitas
};

export type EstadoReservaDTO = {
  idEstadoReserva: number;
  nombreEstado: string;
};

export type EstadoBloqueDTO = {
  idEstado: number;
  nombre: string;
};

export type DiaSemanaDTO = {
  idDia: number;
  nombre: string;
};

export type AgendaBloqueOfertaDTO = {
  idAgenda: number;
  idUsuario: number;
  horaInicio: string;
  horaFinal: string;
  fecha: string;
  estadoBloque: EstadoBloqueDTO;
  diaSemana: DiaSemanaDTO;
};

export type ReservaCreatePayload = {
  idTutorUsuario: number;
  idMascota: number;
  idAgendaBloque: number;
  idTarifa: number;
  /** ISO-8601 sin zona, compatible con {@code LocalDateTime} en el backend (ej. 2026-04-15T18:51:34). */
  fechaSolicitud: string;
  montoTotal: number;
  idEstadoReserva: number;
};

/** Fecha/hora local en formato ISO para que Jackson deserialice a {@code LocalDateTime}. */
export function nowLocalDateTimeISO(): string {
  const d = new Date();
  const z = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${z(d.getMonth() + 1)}-${z(d.getDate())}T${z(d.getHours())}:${z(d.getMinutes())}:${z(d.getSeconds())}`;
}

function fechaSolicitudParaBackend(s: string): string {
  if (/^\d{4}-\d{2}-\d{2}$/.test(s.trim())) return `${s.trim()}T00:00:00`;
  return s;
}

export type ReservaCreatedDTO = {
  idReserva: number;
  idTutorUsuario: number;
  idMascota: number;
  idAgendaBloque: number;
  idTarifa: number;
  idEstadoReserva: number;
  nombreEstado: string;
};

type ReservaBasicaDTO = {
  idReserva: number;
  idTutorUsuario: number;
  idMascota: number;
  idAgendaBloque: number;
  idTarifa?: number | null;
  idEstadoReserva: number | null;
  nombreEstado: string | null;
  fechaSolicitud: string | null;
  fechaAceptacion: string | null;
  montoTotal: number | null;
  idPago: number | null;
  fechaInicioReal: string | null;
  fechaFin: string | null;
  codigoEncuentro: number | null;
};

export type TarifaConfiguracionPublicaDTO = {
  idTarifa: number;
  tamanoId: number;
  tamanoNombre: string;
  precioPorHora: number;
};

export function readTutorIdFromSession(): number {
  const raw = sessionStorage.getItem(TUTOR_ID_SESSION_KEY);
  const id = Number.parseInt(raw ?? "", 10);
  if (!Number.isFinite(id) || id <= 0) {
    throw new Error("No se encontró id de tutor en sesión. Vuelve a iniciar sesión.");
  }
  return id;
}

export async function fetchMascotasTutor(): Promise<MascotaTutorDTO[]> {
  const response = await fetch(API_ENDPOINTS.mascotas.mias, {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudieron cargar tus mascotas."));
  }
  if (!Array.isArray(data)) return [];
  return (data as unknown[]).map((row) => ({
    idMascota: Number((row as { idMascota?: number }).idMascota),
    nombre: String((row as { nombre?: string }).nombre ?? "Mascota"),
    tamanoId: Number(
      ((row as { tamano?: { idTamano?: number } }).tamano as { idTamano?: number } | undefined)?.idTamano ?? NaN
    ) || null,
    tamanoNombre: String(
      ((row as { tamano?: { nombre?: string } }).tamano as { nombre?: string } | undefined)?.nombre ?? ""
    )
  }));
}

export async function fetchPerfilPaseador(idPaseador: number): Promise<PaseadorPerfilDTO> {
  const url = `http://localhost:8080/api/paseadores/public/${idPaseador}`;

  const response = await fetch(url, {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  
  const data = await parseJsonSafe(response);
  
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudo cargar el perfil del paseador para enviar la notificación."));
  }
  
  return data as PaseadorPerfilDTO; 
}
  
  // ... resto del código intacto ...

export async function fetchTarifasPublicasPaseador(
  idPaseador: number
): Promise<TarifaConfiguracionPublicaDTO[]> {
  const response = await fetch(API_ENDPOINTS.auth.paseadores.publicConfiguracion(idPaseador), {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudieron cargar las tarifas del paseador."));
  }
  const tarifasRaw = (data as { tarifas?: unknown[] } | null)?.tarifas;
  if (!Array.isArray(tarifasRaw)) return [];
  return tarifasRaw.map((t) => ({
    idTarifa: Number((t as { idTarifa?: number }).idTarifa),
    tamanoId: Number((t as { tamanoId?: number }).tamanoId),
    tamanoNombre: String((t as { tamanoNombre?: string }).tamanoNombre ?? ""),
    precioPorHora: Number((t as { precioPorHora?: number }).precioPorHora ?? 0)
  }));
}

export async function fetchAgendaOfertaPaseador(
  idPaseador: number,
  desde: string,
  hasta: string
): Promise<AgendaBloqueOfertaDTO[]> {
  const response = await fetch(API_ENDPOINTS.agenda.bloquesOferta(idPaseador, desde, hasta), {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudo cargar la agenda de disponibilidad del paseador."));
  }
  if (!Array.isArray(data)) return [];
  const bloques = data as AgendaBloqueOfertaDTO[];
  // Capa de seguridad en frontend: el tutor solo debe ver bloques en estado DISPONIBLE.
  return bloques.filter((b) => {
    const estado = b.estadoBloque?.nombre?.trim().toUpperCase() ?? "";
    const idEstado = b.estadoBloque?.idEstado;
    return estado.includes("DISPON") || idEstado === 1;
  });
}

export async function fetchEstadoSolicitadaId(): Promise<number> {
  const response = await fetch(API_ENDPOINTS.reserva.estados, {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudieron cargar los estados de reserva."));
  }
  const estados = (Array.isArray(data) ? data : []) as EstadoReservaDTO[];
  const solicitada = estados.find((e) => e.nombreEstado?.trim().toUpperCase() === "SOLICITADA");
  if (!solicitada) {
    throw new Error("No existe el estado SOLICITADA en el catálogo de reserva.");
  }
  return solicitada.idEstadoReserva;
}

export async function crearReservaTutor(payload: ReservaCreatePayload): Promise<ReservaCreatedDTO> {
  const body = {
    ...payload,
    fechaSolicitud: fechaSolicitudParaBackend(payload.fechaSolicitud)
  };
  const response = await fetch(API_ENDPOINTS.reserva.base, {
    method: "POST",
    credentials: "include",
    headers: { ...bearerAuthHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudo crear la reserva."));
  }
  return data as ReservaCreatedDTO;
}

export async function fetchReservasDetalleTutor(idTutor: number): Promise<ReservaTutorDetalleDTO[]> {
  // Endpoint nuevo sin id en URL (usa tutorId del JWT).
  const response = await fetch(API_ENDPOINTS.tutores.bookings, {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (response.ok) {
    return Array.isArray(data) ? (data as ReservaTutorDetalleDTO[]) : [];
  }

  if (response.status === 404) {
    // Compatibilidad con backend previo.
    const legacyDetalle = await fetch(API_ENDPOINTS.reserva.byTutorDetalle(idTutor), {
      method: "GET",
      credentials: "include",
      headers: { ...bearerAuthHeaders() }
    });
    const legacyData = await parseJsonSafe(legacyDetalle);
    if (legacyDetalle.ok) {
      return Array.isArray(legacyData) ? (legacyData as ReservaTutorDetalleDTO[]) : [];
    }
    if (legacyDetalle.status === 404) {
      return fetchReservasBasicasTutor(idTutor);
    }
    throw new Error(readApiErrorMessage(legacyData, "No se pudieron cargar tus reservas."));
  }

  throw new Error(readApiErrorMessage(data, "No se pudieron cargar tus reservas."));
}

async function fetchReservasBasicasTutor(idTutor: number): Promise<ReservaTutorDetalleDTO[]> {
  const response = await fetch(API_ENDPOINTS.reserva.byTutor(idTutor), {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudieron cargar tus reservas."));
  }
  if (!Array.isArray(data)) return [];

  return (data as ReservaBasicaDTO[]).map((reserva) => ({
    idReserva: reserva.idReserva,
    idTutorUsuario: reserva.idTutorUsuario,
    idMascota: reserva.idMascota,
    mascotaNombre: `Mascota #${reserva.idMascota}`,
    idAgendaBloque: reserva.idAgendaBloque,
    idPaseador: null,
    paseadorNombre: `Bloque agenda #${reserva.idAgendaBloque}`,
    fecha: null,
    horaInicio: null,
    horaFinal: null,
    montoTotal: reserva.montoTotal,
    idPago: reserva.idPago,
    idEstadoReserva: reserva.idEstadoReserva,
    nombreEstado: reserva.nombreEstado,
    fechaSolicitud: reserva.fechaSolicitud,
    fechaAceptacion: reserva.fechaAceptacion,
    fechaInicioReal: reserva.fechaInicioReal,
    fechaFin: reserva.fechaFin,
    codigoEncuentro: reserva.codigoEncuentro
  }));
}

export async function cancelarReservaTutor(idReserva: number): Promise<void> {
  const response = await fetch(API_ENDPOINTS.reserva.status(idReserva), {
    method: "PATCH",
    credentials: "include",
    headers: { ...bearerAuthHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify({ tutorDecision: "CANCELAR_SOLICITUD" })
  });
  const data = await parseJsonSafe(response);
  if (response.ok) return;
  throw new Error(readApiErrorMessage(data, "No se pudo cancelar la solicitud."));
}
